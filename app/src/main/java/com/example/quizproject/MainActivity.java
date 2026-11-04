package com.example.quizproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quizproject.Network.ApiClient;
import com.example.quizproject.Network.ApiService;
import com.google.gson.JsonArray;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Button btnHistory;
    private Button btnLibrary;
    private Button btnResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Kiểm tra kết nối Backend EasyQuiz
        testApiConnection();

        // Ánh xạ các nút
        btnHistory = findViewById(R.id.btnHistory);
        btnLibrary = findViewById(R.id.btnLibrary);
        btnResult = findViewById(R.id.btnResult);

        // Màn hình 1: Lịch sử làm bài
        btnHistory.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    HistoryActivity.class
            );

            startActivity(intent);
        });

        // Màn hình 2: Kho đề
        btnLibrary.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    QuizLibraryActivity.class
            );

            startActivity(intent);
        });

        // Màn hình 3: Kết quả bài thi
        btnResult.setOnClickListener(v -> {

            showTokenDialog();

        });
    }

    // Hộp thoại nhập Access Token từ Swagger
    private void showTokenDialog() {

        EditText inputToken = new EditText(this);

        inputToken.setHint("Dán Access Token tại đây");
        inputToken.setSingleLine(true);

        // Dùng để kiểm tra API bài thi số 1
        int attemptId = 1;

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Kết quả bài thi EasyQuiz")
                        .setMessage(
                                "Nhập Access Token từ Swagger " +
                                        "để xem kết quả bài thi số 1."
                        )
                        .setView(inputToken)
                        .setNegativeButton(
                                "Hủy",
                                (d, which) -> d.dismiss()
                        )
                        .setPositiveButton(
                                "Xem kết quả",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            Button btnConfirm = dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            );

            btnConfirm.setOnClickListener(v -> {

                String accessToken = inputToken
                        .getText()
                        .toString()
                        .trim();

                if (accessToken.isEmpty()) {

                    inputToken.setError(
                            "Vui lòng nhập Access Token"
                    );

                    return;
                }

                // Hỗ trợ cả token có hoặc không có Bearer
                if (accessToken.startsWith("Bearer ")) {
                    accessToken = accessToken
                            .substring(7)
                            .trim();
                }

                if (accessToken.isEmpty()) {

                    inputToken.setError(
                            "Access Token không hợp lệ"
                    );

                    return;
                }

                // Mở màn hình kết quả
                Intent intent = new Intent(
                        MainActivity.this,
                        QuizResultActivity.class
                );

                intent.putExtra(
                        "attemptId",
                        attemptId
                );

                intent.putExtra(
                        "accessToken",
                        accessToken
                );

                dialog.dismiss();

                startActivity(intent);
            });
        });

        dialog.show();
    }

    // Kiểm tra kết nối Backend EasyQuiz
    private void testApiConnection() {

        ApiService apiService =
                ApiClient.getClient()
                        .create(ApiService.class);

        apiService.getCategories().enqueue(
                new Callback<JsonArray>() {

                    @Override
                    public void onResponse(
                            Call<JsonArray> call,
                            Response<JsonArray> response) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            Log.d(
                                    "EASYQUIZ_API",
                                    "Kết nối thành công: "
                                            + response.body()
                            );

                            Toast.makeText(
                                    MainActivity.this,
                                    "Kết nối API thành công!",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Log.e(
                                    "EASYQUIZ_API",
                                    "API trả về lỗi: "
                                            + response.code()
                            );

                            Toast.makeText(
                                    MainActivity.this,
                                    "API lỗi: "
                                            + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<JsonArray> call,
                            Throwable t) {

                        Log.e(
                                "EASYQUIZ_API",
                                "Không kết nối được Backend",
                                t
                        );

                        Toast.makeText(
                                MainActivity.this,
                                "Lỗi kết nối: "
                                        + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}
