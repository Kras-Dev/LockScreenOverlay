package com.example.lockscreenoverlay;

// Импортируем базовый класс Service для создания сервиса
import android.app.Service;
// Импортируем Intent для запуска активностей и сервисов
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
// Для задания формата пикселей окна оверлея
import android.content.IntentFilter;
import android.graphics.PixelFormat;
// Интерфейс для привязки сервиса (здесь не используется, возвращаем null)
import android.os.Build;
import android.os.IBinder;
// Для задания расположения окна на экране
import android.view.Gravity;
// Для создания View из XML разметки
import android.view.LayoutInflater;
// Для обработки касаний на View
import android.view.MotionEvent;
// Базовый класс для работы с элементами окна
import android.view.View;
// Для добавления/обновления View в системном окне
import android.view.WindowManager;
// Для вывода кратких уведомлений Toast
import android.widget.Toast;
// Аннотация для Nullable возвращаемого значения
import androidx.annotation.Nullable;

public class FloatButtonService extends Service {
    private WindowManager windowManager;           // Менеджер управления окнами
    private View floatButtonView;                   // Самая кнопка — View из layout_float_button
    private WindowManager.LayoutParams params;     // Параметры расположения кнопки и поведения окна

    private BroadcastReceiver showHideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("ACTION_HIDE_FLOAT_BUTTON".equals(action)) {
                if (floatButtonView != null) {
                    try {
                        windowManager.removeView(floatButtonView);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    floatButtonView = null;
                }
            } else if ("ACTION_SHOW_FLOAT_BUTTON".equals(action)) {
                if (floatButtonView == null) {
                    createAndAddFloatButton();
                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Сервис не поддерживает привязку, возвращаем null
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // Регистрируем BroadcastReceiver с учетом Android 14+
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_HIDE_FLOAT_BUTTON");
        filter.addAction("ACTION_SHOW_FLOAT_BUTTON");
        if (Build.VERSION.SDK_INT >= 34) {
            registerReceiver(showHideReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(showHideReceiver, filter);
        }

        createAndAddFloatButton();
    }
    private void createAndAddFloatButton() {
        // Удаляем предыдущий экземпляр, если он есть
        if (floatButtonView != null) {
            try {
                windowManager.removeView(floatButtonView);
            } catch (Exception ignored) {}
            floatButtonView = null;
        }
        // Загружаем View кнопки из XML разметки
        floatButtonView = LayoutInflater.from(this).inflate(R.layout.layout_float_button, null);

        // Создаем параметры окна для плавающей кнопки
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,      // ширина — под размер контента
                WindowManager.LayoutParams.WRAP_CONTENT,      // высота — под размер контента
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // тип окна — поверх других приложений
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,         // флаг, чтобы окно не перехватывало фокус
                PixelFormat.TRANSLUCENT);                          // прозрачность пикселей окна
        // Располагаем кнопку вверху слева экрана
        params.gravity = Gravity.TOP | Gravity.START;
        // Начальные координаты кнопки
        params.x = 0;
        params.y = 0;
        // Добавляем floatButtonView в окно с заданными параметрами
        windowManager.addView(floatButtonView, params);
        // Показываем сообщение, что кнопка добавлена
        Toast.makeText(getApplicationContext(), "Кнопка блокировки добавлена", Toast.LENGTH_SHORT).show();
        // Устанавливаем слушатель касаний на кнопку для её перемещения и нажатия
        floatButtonView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;           // начальное положение по X
            private int initialY;           // начальное положение по Y
            private float initialTouchX;   // координата касания по X при нажатии
            private float initialTouchY;   // координата касания по Y при нажатии
            private boolean isMoving = false;   // признак, что пользователь двигает кнопку

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    // Пользователь нажал на кнопку
                    case MotionEvent.ACTION_DOWN:
                        // Запоминаем текущие координаты кнопки
                        initialX = params.x;
                        initialY = params.y;
                        // Запоминаем координаты касания
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        // Сброс признака движения
                        isMoving = false;
                        return true;
                    // Пользователь двигает палец по экрану
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);  // смещение по X
                        int deltaY = (int) (event.getRawY() - initialTouchY);  // смещение по Y
                        // Если смещение достаточно большое — считаем, что началось движение
                        if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                            isMoving = true;
                        }
                        // Обновляем координаты кнопки с учётом смещения
                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;
                        // Применяем изменения расположения кнопки на экране
                        windowManager.updateViewLayout(floatButtonView, params);
                        return true;
                    // Пользователь отпустил палец
                    case MotionEvent.ACTION_UP:
                        // Если не было движения — значит, произошло обычное нажатие
                        if (!isMoving) {
                            // Запускаем сервис блокировки экрана
                            Intent intent = new Intent(FloatButtonService.this, LockScreenService.class);
                            startService(intent);
                        }
                        return true;
                }
                return false; // В остальных случаях не обрабатываем событие
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatButtonView != null) {
            try {
                windowManager.removeView(floatButtonView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            floatButtonView = null;
        }
        Toast.makeText(getApplicationContext(), "Кнопка удалена", Toast.LENGTH_SHORT).show();

        unregisterReceiver(showHideReceiver);
    }
}
