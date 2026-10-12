package com.carikostkita.data.repository;

import android.content.Context;
import com.carikostkita.data.model.Review;
import com.carikostkita.data.remote.FastApiReviewService;
import com.carikostkita.data.remote.FastApiSurveyService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.dto.ReviewDto;
import com.carikostkita.data.remote.dto.ReviewListResponse;
import com.carikostkita.data.remote.dto.ReviewResponse;
import com.carikostkita.data.remote.dto.ReviewSubmitRequest;
import com.carikostkita.data.remote.dto.SurveyEligibilityResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

/** Reviews read through FastAPI; writing requires a completed survey for the listing. */
public class ReviewRepository extends BaseRepository {

    private final FastApiReviewService reviewService;
    private final FastApiSurveyService surveyService;

    public ReviewRepository(Context context) {
        super(context);
        reviewService = SupabaseClient.getInstance().createReviewService();
        surveyService = SupabaseClient.getInstance().createSurveyService();
    }

    private String bearer() throws IOException {
        String token = session.getAccessToken();
        if (token == null || token.isEmpty()) throw new IOException("Sesi kamu sudah berakhir. Silakan masuk lagi.");
        if (session.isTokenExpired()) token = SupabaseClient.getInstance().refreshTokenBlocking(token);
        if (token == null || token.isEmpty()) throw new IOException("Sesi kamu sudah berakhir. Silakan masuk lagi.");
        return "Bearer " + token;
    }

    private static long numericId(String value) throws IOException {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException("non-positive ID");
            return id;
        } catch (Exception e) {
            throw new IOException("ID kost tidak valid.", e);
        }
    }

    public void list(String kostId, DataCallback<List<Review>> callback) {
        async("listReviews", callback, () -> {
            Response<ReviewListResponse> response = reviewService
                    .list(null, numericId(kostId), 1, 100).execute();
            ReviewListResponse body = check("listReviews", response);
            List<Review> reviews = new ArrayList<>();
            if (body != null && body.data != null) {
                for (ReviewDto dto : body.data) reviews.add(map(dto));
            }
            return reviews;
        });
    }

    public void canReview(String kostId, DataCallback<Boolean> callback) {
        async("canReview", callback, () -> {
            if (!session.isLoggedIn()) return false;
            Response<SurveyEligibilityResponse> response = surveyService
                    .reviewEligibility(bearer(), numericId(kostId)).execute();
            SurveyEligibilityResponse body = check("canReview", response);
            return body != null && body.eligible;
        });
    }

    public void submit(String kostId, int rating, String komentar, DataCallback<Boolean> callback) {
        async("submitReview", callback, () -> {
            Response<ReviewResponse> response = reviewService.submit(
                    bearer(), numericId(kostId), new ReviewSubmitRequest(rating, komentar)
            ).execute();
            ReviewResponse body = check("submitReview", response);
            return body != null && body.data != null;
        });
    }

    private static Review map(ReviewDto dto) {
        Review review = new Review();
        review.id = dto.id;
        review.kostId = dto.kostId;
        review.namaUser = dto.namaUser;
        review.rating = dto.rating;
        review.komentar = dto.komentar;
        review.createdAt = dto.createdAt;
        return review;
    }
}
