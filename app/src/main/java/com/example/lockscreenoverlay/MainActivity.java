package com.example.lockscreenoverlay;

import android.content.Intent;  // Для работы с Intent - запуска активностей и сервисов
import android.net.Uri;  // Для работы с Uri, например, для перехода в настройки
import android.os.Build;  // Для проверки версии Android
import android.provider.Settings;  // Для доступа к системным настройкам
import android.widget.Toast;  // Для показа кратких всплывающих сообщений
import android.os.Bundle;  // Для передачи данных в методы жизненного цикла активности
import androidx.appcompat.app.AppCompatActivity;  // Базовый класс для активностей с поддержкой ActionBar

public class MainActivity extends AppCompatActivity {
    // Константа запроса разрешения показывать поверх других приложений
    private static final int REQUEST_CODE_OVERLAY = 1000;
    // Метод жизненного цикла, вызываемый при создании активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Вызываем метод родителя для стандартной инициализации
        super.onCreate(savedInstanceState);
        // Устанавливаем разметку главного экрана из XML
        setContentView(R.layout.activity_main);
        // Проверяем, что версия Android 6.0 (API 23) и выше, где требуется явное разрешение на оверлей
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Проверяем, есть ли у приложения разрешение показывать окна поверх других приложений
            if (!Settings.canDrawOverlays(this)){
                // Если разрешение отсутствует, показываем уведомление с просьбой разрешить оверлей
                Toast.makeText(this,
                        "Для работы блокировки экрана нужно разрешение 'Показывать поверх других приложений'. " +
                                "Пожалуйста, разрешите это на следующем экране.",
                        Toast.LENGTH_LONG).show();
                // Создаем Intent для перехода в системные настройки разрешений оверлея
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        // Формируем Uri с текущим пакетом приложения
                        Uri.parse("package:" + getPackageName()));
                // Запускаем системную активность с ожиданием результата (разрешения)
                startActivityForResult(intent, REQUEST_CODE_OVERLAY);

            } else {
                // Если разрешение уже есть, запускаем сервис с плавающей кнопкой
                startFloatButtonService();
                // Завершаем MainActivity, чтобы не мешала
                finish();
            }
        } else {
            // Для Android ниже 6.0 разрешение не требуется, сразу запускаем сервис
            startFloatButtonService();
            // Завершаем MainActivity
            finish();
        }
    }
    // Обработчик результата возврата из настроек разрешений
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        // Проверяем, что возвращаемся именно с запроса разрешения оверлея
        if (requestCode == REQUEST_CODE_OVERLAY){
            // Для безопасности повторно проверяем версию Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Если разрешение получено - запускаем сервис и уведомляем пользователя
                if (Settings.canDrawOverlays(this)){
                    Toast.makeText(this,"Разрешение получено",Toast.LENGTH_SHORT).show();
                    startFloatButtonService();
                    // Завершаем MainActivity
                    finish();

                } else {
                    // Если разрешение не предоставлено — предупреждаем, что функционал не будет работать
                    Toast.makeText(this, "Разрешение не предоставлено, приложение не сможет блокировать экран",
                            Toast.LENGTH_LONG).show();
                    // Завершаем MainActivity
                    finish();
                }
            }
        }
    }
    // Метод жизненного цикла — вызывается, когда активность становится видимой и активной
    @Override
    protected void onResume() {
        super.onResume();
        // Проверяем версию и наличие разрешения при возврате в активность
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                // Запускаем сервис с плавающей кнопкой если разрешение есть
                startFloatButtonService();
                finish();
            }
        }
    }
    // Метод для запуска сервиса с плавающей кнопкой блокировки
    private void startFloatButtonService() {
        // Создаем Intent сервиса плавающей кнопки
        Intent intent = new Intent(this, FloatButtonService.class);
        // Запускаем сервис
        startService(intent);
    }
}