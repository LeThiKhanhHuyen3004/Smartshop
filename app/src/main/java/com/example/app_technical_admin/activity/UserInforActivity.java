package com.example.app_technical_admin.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.model.User;
import com.example.app_technical_admin.model.UserModel;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserInforActivity extends AppCompatActivity {

    private static final String TAG = "UserInforActivity";
    private Toolbar toolbar;
    private ImageView imgAvatar;
    private EditText etUserName, etEmail, etPhoneNumber, etBirthday;
    private AppCompatButton btnSave;
    private Spinner spGender;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private Bitmap avatarBitmap;
    private String userId;
    private ApiSale apiSale;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_infor);

        // Khởi tạo Utils.user_current
        Utils.initUser(this);

        // Kiểm tra và yêu cầu quyền
        checkStoragePermission();

        // Ánh xạ giao diện
        toolbar = findViewById(R.id.toolbar);
        imgAvatar = findViewById(R.id.imgAvatar);
        etUserName = findViewById(R.id.etUserName);
        etEmail = findViewById(R.id.etEmail);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        spGender = findViewById(R.id.spGender);
        etBirthday = findViewById(R.id.etBirthday);
        btnSave = findViewById(R.id.btnSave);

        // Khởi tạo Retrofit
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);

        // Kiểm tra đăng nhập
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = preferences.getString("userId", null);
        if (userId == null || userId.isEmpty() || userId.equals("0") || Utils.user_current == null || Utils.user_current.getId() == 0) {
            Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInforActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Nếu userId không hợp lệ nhưng Utils.user_current có dữ liệu, sử dụng nó
        if (userId == null || userId.isEmpty() || userId.equals("0")) {
            userId = String.valueOf(Utils.user_current.getId());
        }

        // Thiết lập Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Cài đặt Spinner cho giới tính
        setupGenderSpinner();

        // Thiết lập sự kiện cho etBirthday
        etBirthday.setOnClickListener(v -> showDatePicker());

        // Thiết lập sự kiện cho imgAvatar
        imgAvatar.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            } else {
                requestStoragePermission();
            }
        });

        // Thiết lập sự kiện cho btnSave
        btnSave.setOnClickListener(v -> saveUserInfo());

        // Lấy dữ liệu từ server
        fetchUserData();
    }

    private void setupGenderSpinner() {
        ArrayList<String> genderList = new ArrayList<>();
        genderList.add("Male");
        genderList.add("Female");
        genderList.add("Other");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                genderList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);

        // Đặt giá trị mặc định (nếu có từ SharedPreferences)
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String gender = preferences.getString("gender", null);
        if (gender != null) {
            int position = genderList.indexOf(gender);
            if (position >= 0) {
                spGender.setSelection(position);
            }
        }
    }

    private void checkStoragePermission() {
        if (!hasStoragePermission()) {
            requestStoragePermission();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {
                Toast.makeText(this, "Ứng dụng cần quyền truy cập ảnh để chọn avatar", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_CODE);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền truy cập ảnh bị từ chối. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_CODE);
            }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Ứng dụng cần quyền truy cập bộ nhớ để chọn avatar", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền truy cập bộ nhớ bị từ chối. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    private void fetchUserData() {
        // Lưu trữ dữ liệu hiện tại của Utils.user_current để dự phòng
        User currentUserBackup = new User();
        if (Utils.user_current != null) {
            currentUserBackup.setId(Utils.user_current.getId());
            currentUserBackup.setUserName(Utils.user_current.getUserName());
            currentUserBackup.setEmail(Utils.user_current.getEmail());
            currentUserBackup.setPhoneNumber(Utils.user_current.getPhoneNumber());
            currentUserBackup.setGender(Utils.user_current.getGender());
            currentUserBackup.setBirthday(Utils.user_current.getBirthday());
            currentUserBackup.setAvatar(Utils.user_current.getAvatar());
            currentUserBackup.setStatus(Utils.user_current.getStatus());
        }

        compositeDisposable.add(
                apiSale.getUserInfo(userId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                userModel -> {
                                    if (userModel.isSuccess() && userModel.getResult() != null && !userModel.getResult().isEmpty()) {
                                        // Cập nhật Utils.user_current
                                        Utils.user_current = userModel.getResult().get(0);
                                        Log.d(TAG, "Dữ liệu người dùng sau khi fetch: " + Utils.user_current.toString());

                                        // Lưu Utils.user_current vào PaperDB
                                        Utils.saveUser(UserInforActivity.this);
                                        Log.d(TAG, "Đã lưu Utils.user_current vào PaperDB sau khi fetch");

                                        // Hiển thị thông tin lên giao diện
                                        etUserName.setText(Utils.user_current.getUserName() != null ? Utils.user_current.getUserName() : "");
                                        etEmail.setText(Utils.user_current.getEmail() != null ? Utils.user_current.getEmail() : "");
                                        etPhoneNumber.setText(Utils.user_current.getPhoneNumber() != null ? Utils.user_current.getPhoneNumber() : "");
                                        if (Utils.user_current.getGender() != null) {
                                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spGender.getAdapter();
                                            int position = adapter.getPosition(Utils.user_current.getGender());
                                            if (position >= 0) {
                                                spGender.setSelection(position);
                                            }
                                        }
                                        etBirthday.setText(Utils.user_current.getBirthday() != null ? Utils.user_current.getBirthday() : "");

                                        // Lưu thông tin vào SharedPreferences
                                        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString("userId", String.valueOf(Utils.user_current.getId()));
                                        editor.putString("userName", Utils.user_current.getUserName());
                                        editor.putString("email", Utils.user_current.getEmail());
                                        editor.putString("phoneNumber", Utils.user_current.getPhoneNumber());
                                        editor.putString("gender", Utils.user_current.getGender());
                                        editor.putString("birthday", Utils.user_current.getBirthday());
                                        editor.putInt("status", Utils.user_current.getStatus());
                                        editor.apply();

                                        // Hiển thị avatar từ cơ sở dữ liệu
                                        if (Utils.user_current.getAvatar() != null && !Utils.user_current.getAvatar().isEmpty()) {
                                            try {
                                                byte[] decodedString = Base64.decode(Utils.user_current.getAvatar(), Base64.DEFAULT);
                                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                                imgAvatar.setImageBitmap(decodedByte);
                                                avatarBitmap = decodedByte;
                                                Log.d(TAG, "Hiển thị avatar từ cơ sở dữ liệu thành công");
                                            } catch (Exception e) {
                                                Log.e(TAG, "Lỗi giải mã avatar từ cơ sở dữ liệu: " + e.getMessage());
                                                imgAvatar.setImageResource(R.drawable.ic_username);
                                            }
                                        } else {
                                            Log.w(TAG, "Avatar rỗng trong cơ sở dữ liệu");
                                            imgAvatar.setImageResource(R.drawable.ic_username);
                                        }
                                    } else {
                                        Log.w(TAG, "Không tìm thấy thông tin user từ server");
                                        Toast.makeText(this, "Không tìm thấy thông tin user", Toast.LENGTH_SHORT).show();
                                        imgAvatar.setImageResource(R.drawable.ic_username);

                                        // Khôi phục dữ liệu từ bản sao lưu nếu API không trả về dữ liệu
                                        Utils.user_current = currentUserBackup;
                                        Log.d(TAG, "Khôi phục Utils.user_current từ bản sao lưu");
                                    }
                                },
                                throwable -> {
                                    Log.e(TAG, "Lỗi fetchUserData: " + throwable.getMessage(), throwable);
                                    Toast.makeText(this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    imgAvatar.setImageResource(R.drawable.ic_username);

                                    // Khôi phục dữ liệu từ bản sao lưu nếu API thất bại
                                    Utils.user_current = currentUserBackup;
                                    Log.d(TAG, "Khôi phục Utils.user_current từ bản sao lưu do lỗi API");
                                }
                        )
        );
    }

    private void loadUserInfoFromSharedPreferences() {
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userName = preferences.getString("userName", "");
        String email = preferences.getString("email", "");
        String phoneNumber = preferences.getString("phoneNumber", "");
        String gender = preferences.getString("gender", "");
        String birthday = preferences.getString("birthday", "");
        int status = preferences.getInt("status", 0);

        etUserName.setText(userName);
        etEmail.setText(email);
        etPhoneNumber.setText(phoneNumber);
        if (!gender.isEmpty()) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spGender.getAdapter();
            int position = adapter.getPosition(gender);
            if (position >= 0) {
                spGender.setSelection(position);
            }
        }
        etBirthday.setText(birthday);

        // Cập nhật status cho Utils.user_current nếu cần
        if (Utils.user_current != null) {
            Utils.user_current.setStatus(status);
            Log.d(TAG, "Cập nhật status từ SharedPreferences: " + status);
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    etBirthday.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            Log.d(TAG, "Image URI: " + imageUri);
            try {
                avatarBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imgAvatar.setImageBitmap(avatarBitmap);
                Log.d(TAG, "Ảnh đã được chọn và hiển thị");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi tải ảnh: " + e.getMessage(), e);
                Toast.makeText(this, "Không thể tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "Không chọn được ảnh: requestCode=" + requestCode + ", resultCode=" + resultCode);
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        // Nén ảnh để giảm kích thước
        Bitmap compressedBitmap = compressBitmap(bitmap, 500); // Giới hạn chiều rộng/tối đa 500px
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream); // Chất lượng 80%
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String base64String = Base64.encodeToString(byteArray, Base64.DEFAULT);
        Log.d(TAG, "Chuỗi Base64 của avatar: " + base64String.substring(0, Math.min(50, base64String.length())) + "...");
        return base64String;
    }

    private Bitmap compressBitmap(Bitmap originalBitmap, int maxSize) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        if (scale >= 1) return originalBitmap; // Không cần nén nếu ảnh nhỏ hơn maxSize

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
    }

    private void saveUserInfo() {
        String userName = etUserName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String gender = spGender.getSelectedItem().toString();
        String birthday = etBirthday.getText().toString().trim();
        String avatarBase64 = bitmapToBase64(avatarBitmap);

        if (userName.isEmpty() || email.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc (Tên, Email, Số điện thoại)", Toast.LENGTH_SHORT).show();
            return;
        }

        int userIdFromCurrent = Utils.user_current.getId();
        if (userIdFromCurrent <= 0) {
            Log.e(TAG, "userId không hợp lệ từ Utils.user_current: " + userIdFromCurrent);
            Toast.makeText(this, "Không thể cập nhật thông tin: userId không hợp lệ", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInforActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        String userId = String.valueOf(userIdFromCurrent);

        // Lưu bản sao của Utils.user_current trước khi gọi API
        User userBackup = new User();
        userBackup.setId(Utils.user_current.getId());
        userBackup.setUserName(Utils.user_current.getUserName());
        userBackup.setEmail(Utils.user_current.getEmail());
        userBackup.setPhoneNumber(Utils.user_current.getPhoneNumber());
        userBackup.setGender(Utils.user_current.getGender());
        userBackup.setBirthday(Utils.user_current.getBirthday());
        userBackup.setAvatar(Utils.user_current.getAvatar());
        userBackup.setStatus(Utils.user_current.getStatus());
        userBackup.setToken(Utils.user_current.getToken());
        userBackup.setUid(Utils.user_current.getUid());

        Call<UserModel> call = apiSale.updateUserInfo(userId, userName, email, phoneNumber, gender, birthday, avatarBase64 != null ? avatarBase64 : "");
        call.enqueue(new Callback<UserModel>() {
            @Override
            public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserModel userModel = response.body();
                    if (userModel.isSuccess()) {
                        Toast.makeText(UserInforActivity.this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();

                        // Chỉ cập nhật các trường cần thiết thay vì thay thế toàn bộ Utils.user_current
                        if (userModel.getResult() != null && !userModel.getResult().isEmpty()) {
                            User updatedUser = userModel.getResult().get(0);
                            Utils.user_current.setUserName(updatedUser.getUserName());
                            Utils.user_current.setEmail(updatedUser.getEmail());
                            Utils.user_current.setPhoneNumber(updatedUser.getPhoneNumber());
                            Utils.user_current.setGender(updatedUser.getGender());
                            Utils.user_current.setBirthday(updatedUser.getBirthday());
                            Utils.user_current.setAvatar(updatedUser.getAvatar());
                            // Giữ nguyên các trường khác như id, status, token, uid
                        } else {
                            // Nếu API không trả về dữ liệu đầy đủ, sử dụng dữ liệu từ giao diện
                            Utils.user_current.setUserName(userName);
                            Utils.user_current.setEmail(email);
                            Utils.user_current.setPhoneNumber(phoneNumber);
                            Utils.user_current.setGender(gender);
                            Utils.user_current.setBirthday(birthday);
                            Utils.user_current.setAvatar(avatarBase64);
                        }

                        // Lưu vào PaperDB
                        Utils.saveUser(UserInforActivity.this);
                        Log.d(TAG, "Đã lưu Utils.user_current vào PaperDB: " + Utils.user_current.toString());

                        // Cập nhật giao diện
                        if (Utils.user_current.getAvatar() != null && !Utils.user_current.getAvatar().isEmpty()) {
                            byte[] decodedString = Base64.decode(Utils.user_current.getAvatar(), Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            imgAvatar.setImageBitmap(decodedByte);
                            avatarBitmap = decodedByte;
                        }
                    } else {
                        Toast.makeText(UserInforActivity.this, userModel.getMessage(), Toast.LENGTH_SHORT).show();
                        Utils.user_current = userBackup; // Khôi phục nếu API thất bại
                    }
                } else {
                    Log.e(TAG, "Phản hồi không thành công: " + response.code());
                    Toast.makeText(UserInforActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                    Utils.user_current = userBackup; // Khôi phục nếu API thất bại
                }
            }

            @Override
            public void onFailure(Call<UserModel> call, Throwable t) {
                Log.e(TAG, "Lỗi kết nối: " + t.getMessage(), t);
                Toast.makeText(UserInforActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Utils.user_current = userBackup; // Khôi phục nếu API thất bại
            }
        });

        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userId", String.valueOf(Utils.user_current.getId()));
        editor.putString("userName", Utils.user_current.getUserName());
        editor.putString("email", Utils.user_current.getEmail());
        editor.putString("phoneNumber", Utils.user_current.getPhoneNumber());
        editor.putString("gender", Utils.user_current.getGender());
        editor.putString("birthday", Utils.user_current.getBirthday());
        editor.putString("avatar", Utils.user_current.getAvatar());
        editor.putInt("status", Utils.user_current.getStatus());
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền đã được cấp, bạn có thể chọn ảnh!", Toast.LENGTH_SHORT).show();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                    Toast.makeText(this, "Quyền bị từ chối vĩnh viễn. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Quyền bị từ chối. Vui lòng cấp quyền để chọn ảnh.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}