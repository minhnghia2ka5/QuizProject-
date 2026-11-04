package com.example.quizproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizproject.Network.ApiClient;
import com.example.quizproject.Network.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private TextView btnFilterAll, btnFilterHigh;

    private final List<HistoryItem> historyList = new ArrayList<>();

    private boolean highOnly = false;
    private String accessToken = "";

    static class HistoryItem {
        int id;
        String title, date;
        int correct, total, score, coins;

        HistoryItem(int id, String title, String date,
                    int correct, int total,
                    int score, int coins) {
            this.id = id;
            this.title = title;
            this.date = date;
            this.correct = correct;
            this.total = total;
            this.score = score;
            this.coins = coins;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyContainer = findViewById(R.id.historyContainer);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterHigh = findViewById(R.id.btnFilterHigh);

        updateStats();
        showHistory(false);

        // Lọc tất cả bài
        btnFilterAll.setOnClickListener(v -> {
            highOnly = false;
            showHistory(false);
        });

        // Lọc bài có điểm từ 80 trở lên
        btnFilterHigh.setOnClickListener(v -> {
            highOnly = true;
            showHistory(true);
        });

        // Quay lại
        findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());

        // Trang chủ
        findViewById(R.id.navHome)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(
                            this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                });

        // Kho đề
        findViewById(R.id.navLibrary)
                .setOnClickListener(v ->
                        startActivity(new Intent(
                                this, QuizLibraryActivity.class)));

        // Tạo đề - chờ thành viên khác
        findViewById(R.id.navAdd)
                .setOnClickListener(v ->
                        Toast.makeText(
                                this,
                                "Chức năng tạo đề đang phát triển",
                                Toast.LENGTH_SHORT
                        ).show());

        requestAccessToken();
    }

    // Chuyển dp sang pixel
    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    // Background bo góc
    private GradientDrawable background(
            int color, int radius, int stroke) {

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));

        if (stroke != 0) {
            bg.setStroke(dp(1), stroke);
        }

        return bg;
    }

    // Tạo TextView
    private TextView text(
            String value, int size,
            int color, boolean bold) {

        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(color);

        if (bold) {
            tv.setTypeface(null, Typeface.BOLD);
        }

        return tv;
    }

    // Nhập Access Token
    private void requestAccessToken() {

        EditText input = new EditText(this);
        input.setHint("Dán Access Token từ Swagger");
        input.setSingleLine(true);
        input.setPadding(
                dp(20), dp(16),
                dp(20), dp(16));

        new AlertDialog.Builder(this)
                .setTitle("Kết nối lịch sử EasyQuiz")
                .setMessage(
                        "Nhập Access Token để tải lịch sử làm bài.")
                .setView(input)
                .setPositiveButton(
                        "Tải lịch sử",
                        (dialog, which) -> {

                            String token = input.getText()
                                    .toString().trim();

                            if (token.isEmpty()) {
                                Toast.makeText(
                                        this,
                                        "Bạn chưa nhập Access Token",
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            // Tránh Bearer hai lần
                            if (token.startsWith("Bearer ")) {
                                token = token.substring(7).trim();
                            }

                            accessToken = token;
                            loadHistory(token);
                        })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Gọi API lấy lịch sử
    private void loadHistory(String token) {

        historyContainer.removeAllViews();

        TextView loading = text(
                "Đang tải lịch sử làm bài...",
                16, Color.GRAY, false);

        loading.setGravity(Gravity.CENTER);
        loading.setPadding(
                0, dp(40), 0, dp(40));

        historyContainer.addView(loading);

        ApiService api = ApiClient.getClient()
                .create(ApiService.class);

        api.getHistory("Bearer " + token)
                .enqueue(new Callback<JsonArray>() {

                    @Override
                    public void onResponse(
                            Call<JsonArray> call,
                            Response<JsonArray> response) {

                        if (!response.isSuccessful()) {

                            showHistory(highOnly);

                            if (response.code() == 401) {
                                Toast.makeText(
                                        HistoryActivity.this,
                                        "Token không hợp lệ hoặc đã hết hạn",
                                        Toast.LENGTH_LONG
                                ).show();
                            } else {
                                Toast.makeText(
                                        HistoryActivity.this,
                                        "Lỗi API: " + response.code(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                            return;
                        }

                        JsonArray data = response.body();

                        if (data == null) {
                            showHistory(highOnly);
                            Toast.makeText(
                                    HistoryActivity.this,
                                    "Không nhận được dữ liệu",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        List<HistoryItem> loadedItems =
                                new ArrayList<>();

                        try {
                            for (JsonElement element : data) {

                                JsonObject item =
                                        element.getAsJsonObject();

                                // ID dùng để xem lời giải
                                int id = getInt(item, "id");

                                int correct = getInt(
                                        item, "correctCount");
                                int total = getInt(
                                        item, "totalQuestions");
                                int score = getInt(
                                        item, "score");
                                int coins = getInt(
                                        item, "coinsEarned");
                                int seconds = getInt(
                                        item, "durationSeconds");

                                String title = "Bài kiểm tra";

                                if (item.has("category")
                                        && item.get("category")
                                        .isJsonObject()) {

                                    JsonObject category =
                                            item.getAsJsonObject(
                                                    "category");

                                    if (category.has("name")
                                            && !category.get("name")
                                            .isJsonNull()) {

                                        title = category.get("name")
                                                .getAsString();
                                    }
                                }

                                String createdAt = getString(
                                        item, "createdAt");

                                String date = formatDate(createdAt);

                                String duration =
                                        (seconds / 60) + " phút "
                                                + (seconds % 60) + " giây";

                                loadedItems.add(
                                        new HistoryItem(
                                                id,
                                                title,
                                                date + " • " + duration,
                                                correct,
                                                total,
                                                score,
                                                coins
                                        ));
                            }

                            historyList.clear();
                            historyList.addAll(loadedItems);

                            updateStats();
                            showHistory(highOnly);

                            Toast.makeText(
                                    HistoryActivity.this,
                                    "Đã tải " + historyList.size()
                                            + " bài làm",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } catch (Exception e) {

                            showHistory(highOnly);

                            Toast.makeText(
                                    HistoryActivity.this,
                                    "Lỗi xử lý dữ liệu: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<JsonArray> call,
                            Throwable t) {

                        showHistory(highOnly);

                        Toast.makeText(
                                HistoryActivity.this,
                                "Lỗi kết nối: " + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // ===================================================
    // CHỨC NĂNG XEM LỜI GIẢI - GỌI API THEO ID BÀI LÀM
    // ===================================================

    private void showAttemptDetail(HistoryItem historyItem) {

        if (historyItem.id <= 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Không thể xem lời giải")
                    .setMessage(
                            "Bài làm này không có ID hợp lệ.")
                    .setPositiveButton("Đóng", null)
                    .show();
            return;
        }

        if (accessToken.isEmpty()) {
            Toast.makeText(
                    this,
                    "Vui lòng nhập Access Token",
                    Toast.LENGTH_SHORT
            ).show();

            requestAccessToken();
            return;
        }

        // Hộp thoại đang tải
        AlertDialog loadingDialog =
                new AlertDialog.Builder(this)
                        .setTitle("Xem lời giải")
                        .setMessage(
                                "Đang tải chi tiết bài làm...")
                        .setCancelable(false)
                        .create();

        loadingDialog.show();

        ApiService api = ApiClient.getClient()
                .create(ApiService.class);

        api.getAttemptDetail(
                "Bearer " + accessToken,
                historyItem.id
        ).enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(
                    Call<JsonObject> call,
                    Response<JsonObject> response) {

                loadingDialog.dismiss();

                if (!response.isSuccessful()) {

                    String message;

                    if (response.code() == 401) {
                        message =
                                "Access Token không hợp lệ "
                                        + "hoặc đã hết hạn.";
                    } else if (response.code() == 404) {
                        message =
                                "Không tìm thấy bài làm này.";
                    } else {
                        message =
                                "Không thể tải chi tiết bài làm.\n"
                                        + "Mã lỗi: " + response.code();
                    }

                    new AlertDialog.Builder(
                            HistoryActivity.this)
                            .setTitle("Lỗi tải lời giải")
                            .setMessage(message)
                            .setPositiveButton("Đóng", null)
                            .show();

                    return;
                }

                JsonObject detail = response.body();

                if (detail == null) {
                    Toast.makeText(
                            HistoryActivity.this,
                            "API không trả về dữ liệu",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                displayAttemptDialog(detail, historyItem);
            }

            @Override
            public void onFailure(
                    Call<JsonObject> call,
                    Throwable t) {

                loadingDialog.dismiss();

                new AlertDialog.Builder(
                        HistoryActivity.this)
                        .setTitle("Lỗi kết nối")
                        .setMessage(
                                "Không thể kết nối Backend.\n"
                                        + t.getMessage())
                        .setPositiveButton("Đóng", null)
                        .show();
            }
        });
    }

    // Hiển thị hộp thoại chi tiết
    private void displayAttemptDialog(
            JsonObject detail,
            HistoryItem historyItem) {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dp(20), dp(16),
                dp(20), dp(16));

        scrollView.addView(content);

        String title = historyItem.title;

        if (detail.has("category")
                && detail.get("category").isJsonObject()) {

            JsonObject category =
                    detail.getAsJsonObject("category");

            String categoryName =
                    getString(category, "name");

            if (!categoryName.isEmpty()) {
                title = categoryName;
            }
        }

        int correct = getInt(detail, "correctCount");
        int total = getInt(detail, "totalQuestions");
        int score = getInt(detail, "score");
        int coins = getInt(detail, "coinsEarned");
        int seconds = getInt(detail, "durationSeconds");

        // Tiêu đề
        addDialogText(
                content,
                title,
                20,
                Color.rgb(29, 91, 30),
                true);

        addDialogText(
                content,
                "Điểm số: " + score + "/100",
                17,
                Color.BLACK,
                true);

        addDialogText(
                content,
                "Số câu đúng: " + correct + "/" + total,
                15,
                Color.DKGRAY,
                false);

        int accuracy = total == 0 ? 0 :
                Math.round(correct * 100f / total);

        addDialogText(
                content,
                "Tỉ lệ chính xác: " + accuracy + "%",
                15,
                Color.DKGRAY,
                false);

        addDialogText(
                content,
                "Thời gian: "
                        + (seconds / 60) + " phút "
                        + (seconds % 60) + " giây",
                15,
                Color.DKGRAY,
                false);

        addDialogText(
                content,
                "Xu nhận được: +" + coins + " xu",
                15,
                Color.DKGRAY,
                false);

        View divider = new View(this);
        divider.setBackgroundColor(
                Color.rgb(220, 225, 220));

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        -1, dp(1));

        dividerParams.topMargin = dp(16);
        dividerParams.bottomMargin = dp(16);

        content.addView(divider, dividerParams);

        addDialogText(
                content,
                "CHI TIẾT CÂU TRẢ LỜI",
                16,
                Color.rgb(29, 91, 30),
                true);

        // Lấy mảng details từ Backend
        JsonArray details = null;

        if (detail.has("details")
                && detail.get("details").isJsonArray()) {

            details = detail.getAsJsonArray("details");
        }

        if (details == null || details.size() == 0) {

            // Backend hiện tại có thể trả details: []
            addDialogText(
                    content,
                    "Chưa có dữ liệu lời giải từng câu "
                            + "từ Backend.\n\n"
                            + "Bạn vẫn có thể xem điểm số, "
                            + "số câu đúng và thời gian làm bài.",
                    14,
                    Color.GRAY,
                    false);

        } else {

            int index = 1;

            for (JsonElement element : details) {

                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject answer =
                        element.getAsJsonObject();

                // Một số Backend lồng câu hỏi trong
                // thuộc tính question
                JsonObject question = null;

                if (answer.has("question")
                        && answer.get("question")
                        .isJsonObject()) {

                    question = answer.getAsJsonObject(
                            "question");
                }

                String questionText = "";

                if (question != null) {
                    questionText = firstNonEmpty(
                            getString(question, "content"),
                            getString(question, "text"),
                            getString(question, "questionText")
                    );
                }

                if (questionText.isEmpty()) {
                    questionText = firstNonEmpty(
                            getString(answer, "questionText"),
                            getString(answer, "content")
                    );
                }

                if (questionText.isEmpty()) {
                    questionText = "Chưa có nội dung câu hỏi";
                }

                addDialogText(
                        content,
                        "Câu " + index + ": " + questionText,
                        15,
                        Color.BLACK,
                        true);

                // Chỉ hiển thị trường thực sự được
                // Backend trả về, không tự đoán đáp án.
                String selected = firstNonEmpty(
                        getString(answer, "selectedAnswer"),
                        getString(answer, "userAnswer"),
                        getString(answer, "answer")
                );

                String correctAnswer = firstNonEmpty(
                        getString(answer, "correctAnswer"),
                        getString(answer, "answerCorrect")
                );

                String explanation = firstNonEmpty(
                        getString(answer, "explanation"),
                        question == null ? "" :
                                getString(question, "explanation")
                );

                if (!selected.isEmpty()) {
                    addDialogText(
                            content,
                            "Bạn chọn: " + selected,
                            14,
                            Color.DKGRAY,
                            false);
                }

                if (!correctAnswer.isEmpty()) {
                    addDialogText(
                            content,
                            "Đáp án đúng: " + correctAnswer,
                            14,
                            Color.rgb(30, 110, 40),
                            true);
                }

                if (!explanation.isEmpty()) {
                    addDialogText(
                            content,
                            "Giải thích: " + explanation,
                            14,
                            Color.DKGRAY,
                            false);
                }

                if (selected.isEmpty()
                        && correctAnswer.isEmpty()
                        && explanation.isEmpty()) {

                    addDialogText(
                            content,
                            "Backend chưa cung cấp "
                                    + "thông tin đáp án hoặc lời giải "
                                    + "ở định dạng được hỗ trợ.",
                            13,
                            Color.GRAY,
                            false);
                }

                index++;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Xem lời giải bài làm")
                .setView(scrollView)
                .setPositiveButton("Đóng", null)
                .show();
    }

    // Thêm dòng chữ vào hộp thoại
    private void addDialogText(
            LinearLayout parent,
            String value,
            int size,
            int color,
            boolean bold) {

        TextView tv = text(value, size, color, bold);

        tv.setPadding(
                0, dp(5),
                0, dp(5));

        parent.addView(tv);
    }

    private String firstNonEmpty(String... values) {

        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        return "";
    }

    // Đọc số nguyên từ JSON
    private int getInt(JsonObject object, String key) {

        if (object.has(key)
                && !object.get(key).isJsonNull()) {

            try {
                return object.get(key).getAsInt();
            } catch (Exception ignored) {
                return 0;
            }
        }

        return 0;
    }

    // Đọc chuỗi từ JSON
    private String getString(JsonObject object, String key) {

        if (object.has(key)
                && !object.get(key).isJsonNull()) {

            JsonElement element = object.get(key);

            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
        }

        return "";
    }

    // Định dạng ngày
    private String formatDate(String isoDate) {

        if (isoDate == null || isoDate.isEmpty()) {
            return "Không rõ thời gian";
        }

        try {
            SimpleDateFormat input =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                            Locale.US);

            input.setTimeZone(
                    TimeZone.getTimeZone("UTC"));

            Date date = input.parse(isoDate);

            if (date == null) {
                return isoDate;
            }

            SimpleDateFormat output =
                    new SimpleDateFormat(
                            "dd/MM/yyyy, HH:mm",
                            new Locale("vi", "VN"));

            return output.format(date);

        } catch (Exception e) {
            return isoDate;
        }
    }

    // Cập nhật thống kê
    private void updateStats() {

        int correct = 0;
        int total = 0;
        int coins = 0;

        for (HistoryItem item : historyList) {
            correct += item.correct;
            total += item.total;
            coins += item.coins;
        }

        int accuracy = total == 0 ? 0 :
                Math.round(correct * 100f / total);

        ((TextView) findViewById(
                R.id.txtAccuracyPercent))
                .setText(
                        "Tỉ lệ chính xác:\n" + accuracy + "%");

        ((TextView) findViewById(
                R.id.txtTotalQuizzes))
                .setText(
                        historyList.size()
                                + " bài\nĐã hoàn thành");

        ((TextView) findViewById(
                R.id.txtTotalCorrect))
                .setText(
                        correct + " câu\nTrả lời đúng");

        ((TextView) findViewById(
                R.id.txtTotalCoins))
                .setText(
                        "+" + coins + " xu\nTích lũy được");
    }

    // Hiển thị danh sách lịch sử
    private void showHistory(boolean highOnly) {

        historyContainer.removeAllViews();

        btnFilterAll.setBackground(background(
                highOnly ? Color.WHITE :
                        Color.rgb(26, 66, 24),
                30, 0));

        btnFilterAll.setTextColor(
                highOnly ? Color.DKGRAY : Color.WHITE);

        btnFilterHigh.setBackground(background(
                highOnly ? Color.rgb(26, 66, 24) :
                        Color.WHITE,
                30, 0));

        btnFilterHigh.setTextColor(
                highOnly ? Color.WHITE : Color.DKGRAY);

        int count = 0;

        for (HistoryItem item : historyList) {

            if (highOnly && item.score < 80) {
                continue;
            }

            addHistoryCard(item);
            count++;
        }

        if (count == 0) {

            TextView empty = text(
                    "Không có bài thi phù hợp",
                    16, Color.GRAY, false);

            empty.setGravity(Gravity.CENTER);
            empty.setPadding(
                    0, dp(40), 0, dp(40));

            historyContainer.addView(empty);
        }
    }

    // Tạo thẻ lịch sử
    private void addHistoryCard(HistoryItem item) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(
                dp(18), dp(20),
                dp(18), dp(18));

        card.setBackground(background(
                Color.WHITE,
                20,
                Color.rgb(225, 231, 222)));

        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, -2);

        cardParams.bottomMargin = dp(16);

        // Tiêu đề và điểm
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text(
                item.title, 18,
                Color.rgb(28, 34, 29), true);

        heading.addView(
                title,
                new LinearLayout.LayoutParams(
                        0, -2, 1));

        TextView score = text(
                item.score + "/100",
                15,
                Color.rgb(112, 62, 20),
                true);

        score.setGravity(Gravity.CENTER);
        score.setPadding(
                dp(12), dp(7),
                dp(12), dp(7));

        score.setBackground(background(
                Color.rgb(255, 249, 221),
                30, 0));

        heading.addView(score);
        card.addView(heading);

        // Ngày làm bài
        TextView date = text(
                "◷  " + item.date,
                13,
                Color.rgb(110, 118, 111),
                false);

        date.setPadding(
                0, dp(8), 0, dp(14));

        card.addView(date);

        // Nhãn số câu đúng và xu
        LinearLayout badges = new LinearLayout(this);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        badges.setGravity(Gravity.CENTER_VERTICAL);

        TextView correct = text(
                "✓ " + item.correct
                        + "/" + item.total + " đúng",
                13,
                Color.rgb(30, 105, 32),
                true);

        correct.setPadding(
                dp(8), dp(6),
                dp(8), dp(6));

        correct.setBackground(background(
                Color.rgb(237, 246, 235),
                20, 0));

        badges.addView(correct);

        TextView coins = text(
                "🪙 +" + item.coins + " xu",
                13,
                Color.rgb(128, 80, 17),
                true);

        coins.setPadding(
                dp(8), dp(6),
                dp(8), dp(6));

        coins.setBackground(background(
                Color.rgb(255, 249, 228),
                20, 0));

        LinearLayout.LayoutParams coinParams =
                new LinearLayout.LayoutParams(-2, -2);

        coinParams.leftMargin = dp(8);
        badges.addView(coins, coinParams);

        card.addView(badges);

        // Phần trăm chính xác
        int accuracy = item.total == 0 ? 0 :
                Math.round(
                        item.correct * 100f / item.total);

        TextView accuracyText = text(
                "Chính xác: " + accuracy + "%",
                14,
                Color.rgb(31, 94, 25),
                true);

        accuracyText.setGravity(Gravity.END);
        accuracyText.setPadding(
                0, dp(12), 0, dp(8));

        card.addView(accuracyText);

        // Thanh tiến độ
        LinearLayout progressBg =
                new LinearLayout(this);

        progressBg.setOrientation(
                LinearLayout.HORIZONTAL);

        progressBg.setBackground(background(
                Color.rgb(237, 241, 235),
                8, 0));

        LinearLayout progress =
                new LinearLayout(this);

        progress.setBackground(background(
                Color.rgb(42, 111, 23),
                8, 0));

        progressBg.addView(
                progress,
                new LinearLayout.LayoutParams(
                        0, dp(8), accuracy));

        if (accuracy < 100) {

            View remainder = new View(this);

            progressBg.addView(
                    remainder,
                    new LinearLayout.LayoutParams(
                            0, dp(8), 100 - accuracy));
        }

        card.addView(progressBg);

        // Đường phân cách
        View divider = new View(this);

        divider.setBackgroundColor(
                Color.rgb(233, 237, 232));

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        -1, dp(1));

        dividerParams.topMargin = dp(18);
        dividerParams.bottomMargin = dp(14);

        card.addView(divider, dividerParams);

        // Hai nút chức năng
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(
                LinearLayout.HORIZONTAL);

        // Nút Xem lời giải
        TextView review = text(
                "🎓 Xem lời giải",
                14,
                Color.rgb(35, 42, 36),
                true);

        review.setGravity(Gravity.CENTER);
        review.setPadding(
                dp(6), dp(14),
                dp(6), dp(14));

        review.setBackground(background(
                Color.WHITE,
                30,
                Color.rgb(207, 214, 207)));

        buttons.addView(
                review,
                new LinearLayout.LayoutParams(
                        0, -2, 1));

        // Nút Luyện lại
        TextView replay = text(
                "⚡ Luyện lại ⚡",
                14,
                Color.WHITE,
                true);

        replay.setGravity(Gravity.CENTER);
        replay.setPadding(
                dp(6), dp(14),
                dp(6), dp(14));

        replay.setBackground(background(
                Color.rgb(43, 113, 22),
                30, 0));

        LinearLayout.LayoutParams replayParams =
                new LinearLayout.LayoutParams(
                        0, -2, 1);

        replayParams.leftMargin = dp(10);

        buttons.addView(replay, replayParams);

        // ĐÃ TÍCH HỢP API XEM LỜI GIẢI
        review.setOnClickListener(v ->
                showAttemptDetail(item));

        // Chờ màn hình làm bài của thành viên khác
        replay.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Chức năng luyện lại sẽ được "
                                + "kết nối khi ghép dự án",
                        Toast.LENGTH_SHORT
                ).show());

        card.addView(buttons);
        historyContainer.addView(card, cardParams);
    }
}
