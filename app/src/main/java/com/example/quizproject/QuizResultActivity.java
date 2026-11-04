package com.example.quizproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizproject.Network.ApiClient;
import com.example.quizproject.Network.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizResultActivity extends AppCompatActivity {

    private TextView txtAchievementBadge;
    private TextView txtResultTitle;
    private TextView txtResultDescription;
    private TextView txtRewardCoins;
    private TextView txtRewardXp;
    private TextView txtRewardStreak;
    private TextView txtResultAccuracy;
    private TextView txtResultRank;
    private TextView txtResultCorrectSummary;
    private TextView txtCorrectCount;
    private TextView txtWrongCount;
    private TextView txtResultTime;

    private ProgressBar progressAccuracy;

    private int correct = 0;
    private int total = 0;
    private int wrong = 0;
    private int accuracy = 0;
    private int coins = 0;
    private int xp = 0;
    private int streak = 0;

    private int attemptId = -1;
    private String accessToken = "";
    private String duration = "--:--";
    private JsonObject attemptData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        initViews();
        setupButtons();

        Intent intent = getIntent();

        attemptId = intent.getIntExtra("attemptId", -1);

        String token = intent.getStringExtra("accessToken");
        if (token != null) {
            accessToken = token.trim();
        }

        if (attemptId > 0 && !accessToken.isEmpty()) {
            loadResultFromApi();
        } else if (intent.hasExtra("correct")
                && intent.hasExtra("total")) {
            loadResultFromIntent();
            displayResult();
        } else {
            showUnavailableResult();
        }
    }

    private void initViews() {
        txtAchievementBadge =
                findViewById(R.id.txtAchievementBadge);
        txtResultTitle =
                findViewById(R.id.txtResultTitle);
        txtResultDescription =
                findViewById(R.id.txtResultDescription);
        txtRewardCoins =
                findViewById(R.id.txtRewardCoins);
        txtRewardXp =
                findViewById(R.id.txtRewardXp);
        txtRewardStreak =
                findViewById(R.id.txtRewardStreak);
        txtResultAccuracy =
                findViewById(R.id.txtResultAccuracy);
        txtResultRank =
                findViewById(R.id.txtResultRank);
        txtResultCorrectSummary =
                findViewById(R.id.txtResultCorrectSummary);
        txtCorrectCount =
                findViewById(R.id.txtCorrectCount);
        txtWrongCount =
                findViewById(R.id.txtWrongCount);
        txtResultTime =
                findViewById(R.id.txtResultTime);

        progressAccuracy =
                findViewById(R.id.progressAccuracy);
    }

    private String getAuthorization() {
        if (accessToken.isEmpty()) {
            return "";
        }

        return accessToken.startsWith("Bearer ")
                ? accessToken
                : "Bearer " + accessToken;
    }

    // ================================
    // TẢI KẾT QUẢ TỪ BACKEND
    // ================================

    private void loadResultFromApi() {
        txtResultTitle.setText("Đang tải kết quả...");
        txtResultDescription.setText(
                "Đang lấy dữ liệu từ EasyQuiz");

        ApiService api = ApiClient.getClient()
                .create(ApiService.class);

        api.getAttemptDetail(getAuthorization(), attemptId)
                .enqueue(new Callback<JsonObject>() {

                    @Override
                    public void onResponse(
                            Call<JsonObject> call,
                            Response<JsonObject> response) {

                        if (!response.isSuccessful()) {
                            if (response.code() == 401) {
                                showApiError(
                                        "Token không hợp lệ hoặc đã hết hạn");
                            } else {
                                showApiError(
                                        "API trả về mã " + response.code());
                            }
                            return;
                        }

                        JsonObject data = response.body();

                        if (data == null) {
                            showApiError(
                                    "Backend không có dữ liệu");
                            return;
                        }

                        try {
                            attemptData = data;
                            applyAttemptData(data);
                            displayResult();
                        } catch (Exception e) {
                            Log.e("EASYQUIZ_RESULT",
                                    "Lỗi xử lý kết quả", e);
                            showApiError(
                                    "Không đọc được dữ liệu kết quả");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<JsonObject> call,
                            Throwable t) {

                        Log.e("EASYQUIZ_RESULT",
                                "Lỗi kết nối API", t);

                        showApiError(
                                "Không kết nối được Backend");
                    }
                });
    }

    private void applyAttemptData(JsonObject data) {
        total = Math.max(0,
                getInt(data, "totalQuestions", 0));

        correct = Math.max(0,
                Math.min(getInt(data, "correctCount", 0), total));

        wrong = total - correct;

        accuracy = total > 0
                ? Math.round(correct * 100f / total)
                : 0;

        coins = getInt(data, "coinsEarned", 0);
        xp = getInt(data, "xpEarned", 0);
        streak = getInt(data, "streakDays", 0);

        int seconds = getInt(data, "durationSeconds", 0);
        duration = formatDuration(seconds);
    }

    private int getInt(
            JsonObject object, String key, int defaultValue) {

        if (object == null
                || !object.has(key)
                || object.get(key).isJsonNull()) {
            return defaultValue;
        }

        try {
            return object.get(key).getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getString(JsonObject object, String key) {
        if (object == null
                || !object.has(key)
                || object.get(key).isJsonNull()) {
            return "";
        }

        JsonElement element = object.get(key);

        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }

        return "";
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private String formatDuration(int seconds) {
        int safeSeconds = Math.max(0, seconds);

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                safeSeconds / 60,
                safeSeconds % 60);
    }

    // ================================
    // NHẬN DỮ LIỆU TỪ MÀN HÌNH LÀM BÀI
    // ================================

    private void loadResultFromIntent() {
        Intent intent = getIntent();

        total = Math.max(0,
                intent.getIntExtra("total", 0));

        correct = Math.max(0,
                Math.min(intent.getIntExtra("correct", 0), total));

        wrong = total - correct;

        accuracy = total > 0
                ? Math.round(correct * 100f / total)
                : 0;

        coins = intent.getIntExtra("coins", 0);
        xp = intent.getIntExtra("xp", 0);
        streak = intent.getIntExtra("streak", 0);

        String time = intent.getStringExtra("duration");

        duration = time != null && !time.trim().isEmpty()
                ? time : "--:--";
    }

    // ================================
    // HIỂN THỊ KẾT QUẢ
    // ================================

    private void displayResult() {
        txtResultAccuracy.setText(accuracy + "%");
        progressAccuracy.setProgress(accuracy);

        txtResultCorrectSummary.setText(
                correct + " / " + total
                        + " câu trả lời đúng");

        txtCorrectCount.setText(
                "✓\n" + correct + "\nĐúng");

        txtWrongCount.setText(
                "✕\n" + String.format(
                        Locale.getDefault(), "%02d", wrong)
                        + "\nSai");

        txtResultTime.setText(
                "◷\n" + duration + "\nThời gian");

        txtRewardCoins.setText(
                "🪙 +" + coins + " Xu\nThưởng");

        txtRewardXp.setText(
                "⚡ +" + xp + " XP\nKinh nghiệm");

        txtRewardStreak.setText(
                "🔥 " + streak + " Ngày\nChuỗi học");

        // Chỉ đạt 100% mới là Perfect Scorer
        if (accuracy == 100) {
            txtAchievementBadge.setText(
                    "🏆 Tuyệt Đỉnh Hoàn Hảo! 🎉");

            txtResultTitle.setText("Perfect Scorer!");

            txtResultDescription.setText(
                    "Bạn đã trả lời đúng tất cả câu hỏi!");

            txtResultRank.setText(
                    "🏅 Hạng S+ · Hoàn hảo");

        } else if (accuracy >= 90) {
            txtAchievementBadge.setText(
                    "🏆 Thành tích xuất sắc!");

            txtResultTitle.setText("Excellent!");

            txtResultDescription.setText(
                    "Bạn đã hoàn thành bài kiểm tra xuất sắc!");

            txtResultRank.setText(
                    "🏅 Hạng S · Xuất sắc");

        } else if (accuracy >= 80) {
            txtAchievementBadge.setText(
                    "🏆 Thành tích rất tốt!");

            txtResultTitle.setText("Great Job!");

            txtResultDescription.setText(
                    "Bạn đã hoàn thành bài kiểm tra rất tốt!");

            txtResultRank.setText(
                    "🏅 Hạng A · Rất tốt");

        } else if (accuracy >= 60) {
            txtAchievementBadge.setText(
                    "⭐ Hoàn thành bài thi!");

            txtResultTitle.setText("Well Done!");

            txtResultDescription.setText(
                    "Hãy tiếp tục luyện tập để tiến bộ!");

            txtResultRank.setText(
                    "🏅 Hạng B · Khá");

        } else {
            txtAchievementBadge.setText(
                    "🌱 Tiếp tục cố gắng!");

            txtResultTitle.setText("Keep Learning!");

            txtResultDescription.setText(
                    "Luyện tập thêm để nâng cao kết quả!");

            txtResultRank.setText(
                    "🏅 Đang tiến bộ");
        }
    }

    private void showUnavailableResult() {
        txtAchievementBadge.setText("Kết quả EasyQuiz");
        txtResultTitle.setText("Chưa có kết quả");

        txtResultDescription.setText(
                "Hãy hoàn thành một bài thi để xem kết quả.");

        txtResultAccuracy.setText("--%");
        progressAccuracy.setProgress(0);

        txtResultCorrectSummary.setText(
                "Chưa có dữ liệu bài thi");

        txtCorrectCount.setText("✓\n--\nĐúng");
        txtWrongCount.setText("✕\n--\nSai");
        txtResultTime.setText("◷\n--:--\nThời gian");

        txtRewardCoins.setText("🪙 -- Xu\nThưởng");
        txtRewardXp.setText("⚡ -- XP\nKinh nghiệm");
        txtRewardStreak.setText(
                "🔥 -- Ngày\nChuỗi học");

        txtResultRank.setText("Chưa xếp hạng");
    }

    private void showApiError(String message) {
        showUnavailableResult();
        txtResultDescription.setText(message);

        Toast.makeText(
                this, message, Toast.LENGTH_LONG).show();
    }

    // ================================
    // CÁC NÚT CHỨC NĂNG
    // ================================

    private void setupButtons() {
        findViewById(R.id.btnCloseResult)
                .setOnClickListener(v -> openHome());

        findViewById(R.id.btnBackHome)
                .setOnClickListener(v -> openHome());

        findViewById(R.id.btnShareResult)
                .setOnClickListener(v -> shareResult());

        Button btnNextLevel =
                findViewById(R.id.btnNextLevel);

        btnNextLevel.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Chức năng cấp độ tiếp theo đang phát triển",
                        Toast.LENGTH_SHORT
                ).show());

        Button btnReviewAnswers =
                findViewById(R.id.btnReviewAnswers);

        btnReviewAnswers.setOnClickListener(v ->
                reviewAnswers());
    }

    // ================================
    // XEM LẠI ĐÁP ÁN BẰNG API
    // ================================

    private void reviewAnswers() {
        if (attemptId <= 0 || accessToken.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Xem lại đáp án")
                    .setMessage(
                            "Chưa có mã bài làm hoặc Access Token. "
                                    + "Hãy mở kết quả từ bài làm đã lưu "
                                    + "để xem chi tiết đáp án.")
                    .setPositiveButton("Đóng", null)
                    .show();
            return;
        }

        // Nếu đã tải chi tiết từ API, dùng lại dữ liệu
        if (attemptData != null) {
            showReviewDialog(attemptData);
            return;
        }

        AlertDialog loadingDialog =
                new AlertDialog.Builder(this)
                        .setTitle("Xem lại đáp án")
                        .setMessage("Đang tải chi tiết bài làm...")
                        .setCancelable(false)
                        .create();

        loadingDialog.show();

        ApiService api = ApiClient.getClient()
                .create(ApiService.class);

        api.getAttemptDetail(getAuthorization(), attemptId)
                .enqueue(new Callback<JsonObject>() {

                    @Override
                    public void onResponse(
                            Call<JsonObject> call,
                            Response<JsonObject> response) {

                        loadingDialog.dismiss();

                        if (!response.isSuccessful()) {
                            String message = response.code() == 401
                                    ? "Token đã hết hạn hoặc không hợp lệ."
                                    : "Không tải được chi tiết bài làm. Mã lỗi: "
                                      + response.code();

                            showMessageDialog(
                                    "Lỗi xem đáp án", message);
                            return;
                        }

                        JsonObject data = response.body();

                        if (data == null) {
                            showMessageDialog(
                                    "Xem lại đáp án",
                                    "Backend không trả về dữ liệu.");
                            return;
                        }

                        attemptData = data;
                        showReviewDialog(data);
                    }

                    @Override
                    public void onFailure(
                            Call<JsonObject> call,
                            Throwable t) {

                        loadingDialog.dismiss();

                        showMessageDialog(
                                "Lỗi kết nối",
                                "Không kết nối được Backend.");
                    }
                });
    }

    private void showReviewDialog(JsonObject data) {
        ScrollView scrollView = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        int padding = dp(20);
        content.setPadding(
                padding, dp(12), padding, dp(16));

        scrollView.addView(content);

        String categoryName = "Bài kiểm tra";

        if (data.has("category")
                && data.get("category").isJsonObject()) {
            categoryName = firstNonEmpty(
                    getString(
                            data.getAsJsonObject("category"),
                            "name"),
                    categoryName);
        }

        addDialogText(content, categoryName, true);

        int resultCorrect =
                getInt(data, "correctCount", correct);
        int resultTotal =
                getInt(data, "totalQuestions", total);
        int resultCoins =
                getInt(data, "coinsEarned", coins);
        int seconds =
                getInt(data, "durationSeconds", 0);

        int resultAccuracy = resultTotal > 0
                ? Math.round(resultCorrect * 100f / resultTotal)
                : 0;

        addDialogText(
                content,
                "Điểm số: " + getInt(
                        data, "score", resultAccuracy) + "/100",
                true);

        addDialogText(
                content,
                "Số câu đúng: "
                        + resultCorrect + "/" + resultTotal,
                false);

        addDialogText(
                content,
                "Độ chính xác: " + resultAccuracy + "%",
                false);

        addDialogText(
                content,
                "Thời gian: " + formatDuration(seconds),
                false);

        addDialogText(
                content,
                "Xu nhận được: +" + resultCoins,
                false);

        addDialogText(
                content,
                "\nCHI TIẾT CÂU TRẢ LỜI",
                true);

        JsonArray details = null;

        if (data.has("details")
                && data.get("details").isJsonArray()) {
            details = data.getAsJsonArray("details");
        }

        if (details == null || details.size() == 0) {
            addDialogText(
                    content,
                    "Chưa có dữ liệu lời giải từng câu "
                            + "từ Backend.\n\n"
                            + "Bài làm hiện chỉ có thông tin "
                            + "điểm số và thống kê.",
                    false);
        } else {
            int index = 1;

            for (JsonElement element : details) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject answer = element.getAsJsonObject();

                JsonObject question = null;

                if (answer.has("question")
                        && answer.get("question").isJsonObject()) {
                    question =
                            answer.getAsJsonObject("question");
                }

                String questionText = "";

                if (question != null) {
                    questionText = firstNonEmpty(
                            getString(question, "content"),
                            getString(question, "text"),
                            getString(question, "questionText"));
                }

                questionText = firstNonEmpty(
                        questionText,
                        getString(answer, "questionText"),
                        getString(answer, "content"),
                        "Chưa có nội dung câu hỏi");

                addDialogText(
                        content,
                        "\nCâu " + index + ": " + questionText,
                        true);

                String selected = firstNonEmpty(
                        getString(answer, "selectedAnswer"),
                        getString(answer, "userAnswer"));

                String correctAnswer = firstNonEmpty(
                        getString(answer, "correctAnswer"),
                        getString(answer, "answerCorrect"));

                String explanation = firstNonEmpty(
                        getString(answer, "explanation"),
                        question == null ? ""
                                : getString(question, "explanation"));

                if (!selected.isEmpty()) {
                    addDialogText(
                            content,
                            "Bạn chọn: " + selected,
                            false);
                }

                if (!correctAnswer.isEmpty()) {
                    addDialogText(
                            content,
                            "Đáp án đúng: " + correctAnswer,
                            false);
                }

                if (!explanation.isEmpty()) {
                    addDialogText(
                            content,
                            "Giải thích: " + explanation,
                            false);
                }

                if (selected.isEmpty()
                        && correctAnswer.isEmpty()
                        && explanation.isEmpty()) {
                    addDialogText(
                            content,
                            "Backend chưa cung cấp thông tin "
                                    + "đáp án theo định dạng được hỗ trợ.",
                            false);
                }

                index++;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Xem lại đáp án")
                .setView(scrollView)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private void addDialogText(
            LinearLayout parent, String message, boolean bold) {

        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(bold ? 16 : 14);
        textView.setTextColor(
                bold ? 0xFF205B27 : 0xFF444444);

        if (bold) {
            textView.setTypeface(
                    null, android.graphics.Typeface.BOLD);
        }

        textView.setPadding(0, dp(5), 0, dp(5));
        parent.addView(textView);
    }

    private void showMessageDialog(
            String title, String message) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đóng", null)
                .show();
    }

    // ================================
    // CHIA SẺ KẾT QUẢ
    // ================================

    private void shareResult() {
        if (total <= 0) {
            Toast.makeText(
                    this,
                    "Chưa có kết quả để chia sẻ",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String message =
                "🏆 Kết quả EasyQuiz\n"
                        + "Đúng: " + correct + "/" + total + "\n"
                        + "Độ chính xác: " + accuracy + "%";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);

        startActivity(Intent.createChooser(
                intent, "Chia sẻ kết quả"));
    }

    // ================================
    // QUAY VỀ TRANG CHỦ
    // ================================

    private void openHome() {
        Intent intent = new Intent(
                this, MainActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
        finish();
    }
}
