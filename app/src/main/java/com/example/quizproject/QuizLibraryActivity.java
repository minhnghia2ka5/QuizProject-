package com.example.quizproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizproject.Network.ApiClient;
import com.example.quizproject.Network.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizLibraryActivity extends AppCompatActivity {

    private LinearLayout libraryContainer;
    private TextView filterAll, filterAi, filterWord;
    private TextView txtQuizCount, txtSubtitle;
    private EditText searchEditText;

    private final List<QuizItem> quizzes = new ArrayList<>();
    private String selectedFilter = "ALL";
    private boolean loading = false;
    private String errorMessage = "";

    static class QuizItem {
        int id, questionCount, highScore;
        String title, description;

        QuizItem(int id, String title, String description,
                 int questionCount, int highScore) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.questionCount = questionCount;
            this.highScore = highScore;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_library);

        libraryContainer = findViewById(R.id.libraryContainer);
        filterAll = findViewById(R.id.filterAllQuizzes);
        filterAi = findViewById(R.id.filterAiQuizzes);
        filterWord = findViewById(R.id.filterWordQuizzes);
        txtQuizCount = findViewById(R.id.txtQuizCount);
        txtSubtitle = findViewById(R.id.txtLibrarySubtitle);
        searchEditText = findViewById(R.id.searchEditText);

        filterAll.setOnClickListener(v -> {
            selectedFilter = "ALL";
            renderQuizzes();
        });

        filterAi.setOnClickListener(v -> {
            selectedFilter = "AI";
            renderQuizzes();
        });

        filterWord.setOnClickListener(v -> {
            selectedFilter = "WORD";
            renderQuizzes();
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start,
                    int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start,
                    int before, int count) {
                renderQuizzes();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnLibraryBack)
                .setOnClickListener(v -> finish());

        findViewById(R.id.navLibraryHome)
                .setOnClickListener(v -> openHome());

        findViewById(R.id.navLibraryHistory)
                .setOnClickListener(v ->
                        startActivity(new Intent(
                                this, HistoryActivity.class)));

        View.OnClickListener createListener = v ->
                Toast.makeText(this,
                        "Chức năng tạo đề đang phát triển",
                        Toast.LENGTH_SHORT).show();

        findViewById(R.id.btnAddQuiz)
                .setOnClickListener(createListener);

        findViewById(R.id.navLibraryAdd)
                .setOnClickListener(createListener);

        findViewById(R.id.btnCreateNow)
                .setOnClickListener(createListener);

        findViewById(R.id.btnSearchFilter)
                .setOnClickListener(v ->
                        Toast.makeText(this,
                                "Chọn bộ lọc phía dưới",
                                Toast.LENGTH_SHORT).show());

        // Tải dữ liệu thật khi mở màn hình
        loadCategories();
    }

    private void loadCategories() {
        loading = true;
        errorMessage = "";
        renderQuizzes();

        ApiService api = ApiClient.getClient()
                .create(ApiService.class);

        api.getCategories().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(
                    Call<JsonArray> call,
                    Response<JsonArray> response) {

                loading = false;

                if (!response.isSuccessful()) {
                    errorMessage = "Lỗi API: " + response.code();
                    renderQuizzes();
                    return;
                }

                JsonArray data = response.body();

                if (data == null) {
                    errorMessage = "Backend không trả về dữ liệu";
                    renderQuizzes();
                    return;
                }

                List<QuizItem> newItems = new ArrayList<>();

                try {
                    for (JsonElement element : data) {
                        JsonObject object =
                                element.getAsJsonObject();

                        int id = getInt(object, "id");
                        String name = getString(
                                object, "name");
                        String description = getString(
                                object, "description");
                        int count = getInt(
                                object, "questionCount");
                        int highScore = getInt(
                                object, "highScore");

                        newItems.add(new QuizItem(
                                id,
                                name,
                                description,
                                count,
                                highScore
                        ));
                    }

                    quizzes.clear();
                    quizzes.addAll(newItems);
                    errorMessage = "";

                    Log.d("EASYQUIZ_LIBRARY",
                            "Đã tải " + quizzes.size()
                                    + " chủ đề");

                    renderQuizzes();

                } catch (Exception e) {
                    errorMessage = "Lỗi đọc dữ liệu API";
                    Log.e("EASYQUIZ_LIBRARY",
                            "Lỗi xử lý JSON", e);
                    renderQuizzes();
                }
            }

            @Override
            public void onFailure(
                    Call<JsonArray> call,
                    Throwable t) {

                loading = false;
                errorMessage = "Không kết nối được Backend";

                Log.e("EASYQUIZ_LIBRARY",
                        "Lỗi kết nối API", t);

                renderQuizzes();
            }
        });
    }

    private int getInt(JsonObject object, String key) {
        if (object.has(key)
                && !object.get(key).isJsonNull()) {
            return object.get(key).getAsInt();
        }
        return 0;
    }

    private String getString(JsonObject object, String key) {
        if (object.has(key)
                && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return "";
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private GradientDrawable bg(
            int color, int radius, int stroke) {

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        if (stroke != 0) {
            drawable.setStroke(dp(1), stroke);
        }

        return drawable;
    }

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

    private void setFilterStyle(
            TextView view, boolean active) {

        view.setBackground(bg(
                active ? Color.rgb(25, 75, 23) : Color.WHITE,
                30,
                active ? 0 : Color.rgb(225, 230, 225)));

        view.setTextColor(
                active ? Color.WHITE
                        : Color.rgb(52, 58, 53));
    }

    private void renderQuizzes() {
        libraryContainer.removeAllViews();

        setFilterStyle(filterAll,
                selectedFilter.equals("ALL"));

        setFilterStyle(filterAi,
                selectedFilter.equals("AI"));

        setFilterStyle(filterWord,
                selectedFilter.equals("WORD"));

        txtSubtitle.setText(
                quizzes.size() + " chủ đề từ EasyQuiz");

        String keyword = searchEditText.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (loading) {
            txtQuizCount.setText("Đang tải...");

            showMessage("Đang tải chủ đề từ Backend...");
            return;
        }

        if (!errorMessage.isEmpty()) {
            txtQuizCount.setText("Lỗi tải dữ liệu");

            showMessage(errorMessage);

            TextView retry = text(
                    "Thử tải lại",
                    15, Color.WHITE, true);

            retry.setGravity(Gravity.CENTER);
            retry.setPadding(
                    dp(12), dp(14),
                    dp(12), dp(14));

            retry.setBackground(bg(
                    Color.rgb(42, 111, 23), 20, 0));

            retry.setOnClickListener(v ->
                    loadCategories());

            libraryContainer.addView(retry);
            return;
        }

        // API categories không cung cấp nguồn AI/Word
        if (!selectedFilter.equals("ALL")) {
            txtQuizCount.setText("Chưa có dữ liệu");

            showMessage(
                    "Backend chưa cung cấp thông tin "
                            + "đề tạo từ AI hoặc Word.");
            return;
        }

        int visibleCount = 0;

        for (QuizItem item : quizzes) {
            boolean matchesSearch =
                    item.title.toLowerCase(Locale.ROOT)
                            .contains(keyword)
                            || item.description
                            .toLowerCase(Locale.ROOT)
                            .contains(keyword);

            if (matchesSearch) {
                addQuizCard(item);
                visibleCount++;
            }
        }

        txtQuizCount.setText(
                visibleCount + " chủ đề");

        if (visibleCount == 0) {
            showMessage("Không tìm thấy chủ đề phù hợp");
        }
    }

    private void showMessage(String message) {
        TextView empty = text(
                message, 15, Color.GRAY, false);

        empty.setGravity(Gravity.CENTER);
        empty.setPadding(
                dp(16), dp(40),
                dp(16), dp(40));

        libraryContainer.addView(empty);
    }

    private void addQuizCard(QuizItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(
                dp(18), dp(20),
                dp(18), dp(20));

        card.setBackground(bg(
                Color.WHITE, 26,
                Color.rgb(225, 229, 225)));

        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, -2);

        cardParams.bottomMargin = dp(16);

        // Phần đầu thẻ
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = text(
                "📚", 28,
                Color.rgb(100, 70, 15), true);

        icon.setGravity(Gravity.CENTER);

        icon.setBackground(bg(
                Color.rgb(255, 246, 201),
                18, 0));

        header.addView(icon,
                new LinearLayout.LayoutParams(
                        dp(60), dp(60)));

        TextView category = text(
                "Chủ đề #" + item.id,
                14, Color.rgb(35, 41, 35), true);

        LinearLayout.LayoutParams categoryParams =
                new LinearLayout.LayoutParams(
                        0, -2, 1);

        categoryParams.leftMargin = dp(12);
        header.addView(category, categoryParams);

        TextView source = text(
                "EasyQuiz", 13,
                Color.WHITE, true);

        source.setGravity(Gravity.CENTER);
        source.setPadding(
                dp(10), dp(10),
                dp(10), dp(10));

        source.setBackground(bg(
                Color.rgb(27, 70, 28),
                30, 0));

        header.addView(source);
        card.addView(header);

        // Tên chủ đề
        TextView title = text(
                item.title, 21,
                Color.rgb(27, 34, 27), true);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(-1, -2);

        titleParams.topMargin = dp(24);
        titleParams.bottomMargin = dp(12);

        card.addView(title, titleParams);

        // Mô tả
        TextView description = text(
                item.description.isEmpty()
                        ? "Chưa có mô tả"
                        : item.description,
                14,
                Color.rgb(78, 87, 78),
                false);

        card.addView(description);

        // Số câu hỏi
        TextView count = text(
                "📝 " + item.questionCount
                        + " câu hỏi",
                14,
                Color.rgb(78, 87, 78),
                false);

        count.setPadding(
                0, dp(14), 0, dp(8));

        card.addView(count);

        // Điểm cao nhất
        TextView score = text(
                "⭐ Điểm cao nhất: "
                        + item.highScore,
                14,
                Color.rgb(78, 87, 78),
                false);

        score.setPadding(
                0, 0, 0, dp(18));

        card.addView(score);

        View divider = new View(this);
        divider.setBackgroundColor(
                Color.rgb(226, 230, 226));

        card.addView(divider,
                new LinearLayout.LayoutParams(
                        -1, dp(1)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams actionsParams =
                new LinearLayout.LayoutParams(-1, -2);

        actionsParams.topMargin = dp(18);

        TextView play = text(
                "▷ Vào thi ngay",
                14, Color.WHITE, true);

        play.setGravity(Gravity.CENTER);
        play.setPadding(
                dp(8), dp(15),
                dp(8), dp(15));

        play.setBackground(bg(
                Color.rgb(37, 113, 23),
                30, 0));

        actions.addView(play,
                new LinearLayout.LayoutParams(
                        0, -2, 1.5f));

        TextView details = text(
                "⚙ Chi tiết",
                14,
                Color.rgb(39, 47, 39),
                true);

        details.setGravity(Gravity.CENTER);
        details.setPadding(
                dp(8), dp(15),
                dp(8), dp(15));

        details.setBackground(bg(
                Color.WHITE, 30,
                Color.rgb(216, 221, 216)));

        LinearLayout.LayoutParams detailParams =
                new LinearLayout.LayoutParams(
                        0, -2, 1);

        detailParams.leftMargin = dp(8);
        actions.addView(details, detailParams);

        TextView delete = text(
                "🗑", 20,
                Color.rgb(185, 46, 46),
                true);

        delete.setGravity(Gravity.CENTER);

        delete.setBackground(bg(
                Color.rgb(255, 246, 245),
                30,
                Color.rgb(245, 208, 203)));

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        dp(48), dp(48));

        deleteParams.leftMargin = dp(8);
        actions.addView(delete, deleteParams);

        card.addView(actions, actionsParams);

        play.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Chức năng làm bài do thành viên "
                                + "khác tích hợp",
                        Toast.LENGTH_SHORT).show());

        details.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(item.title)
                        .setMessage(
                                "Mã chủ đề: " + item.id
                                        + "\nMô tả: " + item.description
                                        + "\nSố câu hỏi: "
                                        + item.questionCount
                                        + "\nĐiểm cao nhất: "
                                        + item.highScore)
                        .setPositiveButton(
                                "Đóng", null)
                        .show());

        // Không xóa chủ đề thật trên Backend
        delete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Chưa hỗ trợ xóa")
                        .setMessage(
                                "Đây là chủ đề từ Backend. "
                                        + "Chức năng xóa đề cá nhân "
                                        + "cần API riêng.")
                        .setPositiveButton(
                                "Đã hiểu", null)
                        .show());

        libraryContainer.addView(
                card, cardParams);
    }

    private void openHome() {
        Intent intent = new Intent(
                this, MainActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
    }
}
