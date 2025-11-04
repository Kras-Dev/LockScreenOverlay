package com.example.lockscreenoverlay;
// Импортируем базовый класс Service для создания сервиса
import android.app.Service;
// Для запуска активностей и сервисов через Intent
import android.content.Intent;
// Для задания формата пикселей окна оверлея
import android.graphics.PixelFormat;
// Интерфейс для привязки сервиса (здесь не используется, возвращаем null)
import android.os.IBinder;
// Для создания View из XML разметки
import android.view.LayoutInflater;
// Для обработки касаний на View (свайпы, жесты)
import android.view.MotionEvent;
// Базовый класс для View элементов UI
import android.view.View;
// Для управления окнами на экране (добавление, обновление, удаление)
import android.view.WindowManager;
// Для работы с кнопками UI
import android.widget.Button;
// Аннотация для Nullable возвращаемого значения
import androidx.annotation.Nullable;

public class LockScreenService extends Service {
    private WindowManager windowManager; // Менеджер управления окнами
    private View lockScreenView;         // View блокирующего экрана

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Сервис не поддерживает привязку, возвращаем null
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Получаем системный сервис для управления окнами
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // Загружаем View блокирующего экрана из XML разметки
        lockScreenView = LayoutInflater.from(this).inflate(R.layout.layout_lock_screen, null);
        // Скрыть плавающую кнопку при показе экрана блокировки
        Intent hideIntent = new Intent("ACTION_HIDE_FLOAT_BUTTON");
        sendBroadcast(hideIntent);
        // Получаем область свайпа для отслеживания движений пользователя
        final View swipeZone = lockScreenView.findViewById(R.id.swipe_area);
        // Получаем круглый индикатор, который пользователь будет перетаскивать
        final View dragIndicator = lockScreenView.findViewById(R.id.drag_indicator);
        // Устанавливаем слушатель касаний на область свайпа
        swipeZone.setOnTouchListener(new View.OnTouchListener() {
            private float startX;               // Начальная координата X при касании
            private boolean dragging = false;  // Флаг — происходит ли перетаскивание
            private final int dragThreshold = 500; // Порог растяжения для разблокировки (в px)
            private float originalX;            // Исходная координата X индикатора

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    // Начало касания
                    startX = event.getX();                 // Запоминаем позицию касания
                    dragging = true;                      // Запускаем режим перетаскивания
                    originalX = dragIndicator.getX();     // Запоминаем текущую позицию индикатора
                    return true;

                    // Перемещение пальца по экрану
                    case MotionEvent.ACTION_MOVE:
                        if (!dragging) return false;          // Если перетаскивание не в процессе — игнорируем
                        float currentX = event.getX();        // Текущая позиция пальца
                        float deltaX = currentX - startX;     // Расстояние перемещения по X

                        // Вычисляем новую позицию индикатора, ограничиваем внутри зоны свайпа
                        float newX = originalX + deltaX;
                        if (newX < swipeZone.getLeft()) newX = swipeZone.getLeft();
                        if (newX > swipeZone.getRight() - dragIndicator.getWidth())
                            newX = swipeZone.getRight() - dragIndicator.getWidth();

                        dragIndicator.setX(newX);              // Обновляем позицию индикатора

                        // Если смещение превысило пороговое значение — считаем экран разблокированным
                        if (deltaX > dragThreshold) {
                            dragging = false;                  // Останавливаем перетаскивание
                            stopSelf();                       // Останавливаем сервис блокировки экрана
                            startService(new Intent(LockScreenService.this, FloatButtonService.class)); // Запускаем сервис плавающей кнопки
                            return true;
                        }
                        return true;

                    // Отпускание пальца или отмена жеста
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        dragging = false;                      // Останавливаем перетаскивание
                        dragIndicator.setX(originalX);         // Возвращаем индикатор в исходную позицию
                        return true;
                }
                return false;  // Остальные события не обрабатываем
            }
        });

        // Получаем кнопку закрытия экрана блокировки
        Button closeButton = lockScreenView.findViewById(R.id.close_button);
        // Устанавливаем обработчик нажатия на кнопку закрытия
        closeButton.setOnClickListener(v -> {
            stopSelf();  // Останавливаем сервис блокировки
            // При закрытии шоу обратно плавающую кнопку
            Intent showIntent = new Intent("ACTION_SHOW_FLOAT_BUTTON");
            sendBroadcast(showIntent);
            Intent floatIntent = new Intent(LockScreenService.this, FloatButtonService.class);
            stopService(floatIntent);  // Останавливаем сервис плавающей кнопки (если был запущен)
            android.os.Process.killProcess(android.os.Process.myPid());  // Завершаем процесс приложения
        });
        // Параметры окна для блокирующего экрана: полный размер экрана, тип оверлея, флаги окна
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,  // Во всю ширину экрана
                WindowManager.LayoutParams.MATCH_PARENT,  // Во всю высоту экрана
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // Окно уровня оверлей
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE // Окно не перехватывает фокус
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL   // Позволяет получать касания вне окна
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,   // На весь экран
                PixelFormat.TRANSLUCENT);  // Прозрачный пиксельный формат

        // Добавляем наш экран блокировки в окно системы
        windowManager.addView(lockScreenView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Удаляем экран блокировки с экрана, если он есть
        if (lockScreenView != null) {
            windowManager.removeView(lockScreenView);
            // Освобождаем ссылку
            lockScreenView = null;
        }
        // При уничтожении сервиса тоже показываем плавающую кнопку
        Intent showIntent = new Intent("ACTION_SHOW_FLOAT_BUTTON");
        sendBroadcast(showIntent);
    }
}
