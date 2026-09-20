
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cat.narezany.margyt.plugin.MargyPlugin;

/**
 * Video Quality Picker — MargyT plugin v2.11
 * Автор: fodis3
 *
 * - Меню выбора качества в чистом дизайне Material 3 Expressive с векторными иконками (без эмодзи/стикеров).
 * - Интеллектуальное определение фотопостов (каруселей фото) TikTok: удобное скачивание звука ролика (MP3).
 * - По умолчанию сверху выводится лучшее качество со звёздочкой, остальные качества скрыты в раскрывающемся списке.
 * - Приятная тактильная отдача (вибрация) при взаимодействии с плагином.
 * - Возможность отдельного скачивания аудиодорожки/звука ролика (MP3).
 * - Вариант с водяным знаком строго в самом конце.
 * - Юзернейм (username) автора ролика корректно добавляется в начало названия файла без черточек.
 * - Кнопка настроек прямо в шапке окна скачивания, по долгому нажатию на плавающую кнопку и в настройках MargyT.
 * - Настройка пути сохранения видео (выбор папки Загрузки, TikTok, MargyT, Фильмы или своя папка).
 * - Настройка размера и прозрачности кнопки.
 */
public final class VideoQualityPicker extends MargyPlugin {

    // ─────────────────────────────────────────── vector icons ─────────────────

    /** Векторная иконка скачивания (стрелка вниз + лоток) */
    private static class DownloadIconDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int color;

        DownloadIconDrawable(int color) {
            this.color = color;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        public void setColor(int c) {
            this.color = c;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            if (w <= 0 || h <= 0) return;

            paint.setColor(color);
            float stroke = Math.max(2.2f, w * 0.12f);
            paint.setStrokeWidth(stroke);

            float cx = b.left + w * 0.5f;
            float topY = b.top + h * 0.16f;
            float tipY = b.top + h * 0.58f;
            canvas.drawLine(cx, topY, cx, tipY, paint);

            float wing = w * 0.22f;
            canvas.drawLine(cx - wing, tipY - wing, cx, tipY, paint);
            canvas.drawLine(cx + wing, tipY - wing, cx, tipY, paint);

            float trayY = b.top + h * 0.82f;
            float trayPad = w * 0.18f;
            float trayWing = h * 0.14f;
            Path tray = new Path();
            tray.moveTo(b.left + trayPad, trayY - trayWing);
            tray.lineTo(b.left + trayPad, trayY);
            tray.lineTo(b.right - trayPad, trayY);
            tray.lineTo(b.right - trayPad, trayY - trayWing);
            canvas.drawPath(tray, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Векторная шестерёнка для настроек */
    private static class GearIconDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int color;

        GearIconDrawable(int color) {
            this.color = color;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        public void setColor(int c) {
            this.color = c;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            if (w <= 0 || h <= 0) return;

            paint.setColor(color);
            float stroke = Math.max(2.0f, w * 0.12f);
            paint.setStrokeWidth(stroke);

            float cx = b.left + w * 0.5f;
            float cy = b.top + h * 0.5f;
            float r = w * 0.26f;

            canvas.drawCircle(cx, cy, r, paint);

            float tooth = w * 0.14f;
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i * 60);
                float x1 = (float) (cx + (r + 1) * Math.cos(a));
                float y1 = (float) (cy + (r + 1) * Math.sin(a));
                float x2 = (float) (cx + (r + tooth) * Math.cos(a));
                float y2 = (float) (cy + (r + tooth) * Math.sin(a));
                canvas.drawLine(x1, y1, x2, y2, paint);
            }
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Векторная пятиконечная звезда */
    private static class StarIconDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int color;

        StarIconDrawable(int color) {
            this.color = color;
            paint.setStyle(Paint.Style.FILL);
        }

        public void setColor(int c) {
            this.color = c;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            if (w <= 0 || h <= 0) return;

            paint.setColor(color);
            float cx = b.left + w * 0.5f;
            float cy = b.top + h * 0.5f;
            float rOut = w * 0.46f;
            float rIn = rOut * 0.46f;

            Path path = new Path();
            for (int i = 0; i < 10; i++) {
                double a = Math.toRadians(i * 36 - 90);
                float r = (i % 2 == 0) ? rOut : rIn;
                float x = (float) (cx + r * Math.cos(a));
                float y = (float) (cy + r * Math.sin(a));
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            path.close();
            canvas.drawPath(path, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Векторный шеврон для раскрытия/скрытия списка */
    private static class ChevronIconDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int color;
        private boolean isExpanded;

        ChevronIconDrawable(int color, boolean isExpanded) {
            this.color = color;
            this.isExpanded = isExpanded;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        public void setExpanded(boolean exp) {
            this.isExpanded = exp;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            if (w <= 0 || h <= 0) return;

            paint.setColor(color);
            paint.setStrokeWidth(Math.max(2.0f, w * 0.14f));

            float cx = b.left + w * 0.5f;
            float cy = b.top + h * 0.5f;
            float hw = w * 0.35f;
            float hh = h * 0.22f;

            Path p = new Path();
            if (isExpanded) {
                p.moveTo(cx - hw, cy + hh);
                p.lineTo(cx, cy - hh);
                p.lineTo(cx + hw, cy + hh);
            } else {
                p.moveTo(cx - hw, cy - hh);
                p.lineTo(cx, cy + hh);
                p.lineTo(cx + hw, cy - hh);
            }
            canvas.drawPath(p, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Векторный крестик для отмены скачивания (✕) */
    private static class CrossIconDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int color;

        CrossIconDrawable(int color) {
            this.color = color;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        public void setColor(int c) {
            this.color = c;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            if (w <= 0 || h <= 0) return;

            paint.setColor(color);
            paint.setStrokeWidth(Math.max(2.2f, w * 0.13f));

            float pad = w * 0.28f;
            canvas.drawLine(b.left + pad, b.top + pad, b.right - pad, b.bottom - pad, paint);
            canvas.drawLine(b.right - pad, b.top + pad, b.left + pad, b.bottom - pad, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Векторная иконка папки для системного проводника */
    private static class FolderIconDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int color;

        FolderIconDrawable(int color) {
            this.color = color;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        public void setColor(int c) {
            this.color = c;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            if (w <= 0 || h <= 0) return;

            paint.setColor(color);
            paint.setStrokeWidth(Math.max(2.0f, w * 0.11f));

            float padX = w * 0.15f;
            float topY = b.top + h * 0.28f;
            float botY = b.bottom - h * 0.22f;
            float leftX = b.left + padX;
            float rightX = b.right - padX;

            Path p = new Path();
            p.moveTo(leftX, topY);
            p.lineTo(leftX + w * 0.25f, topY);
            p.lineTo(leftX + w * 0.35f, topY + h * 0.12f);
            p.lineTo(rightX, topY + h * 0.12f);
            p.lineTo(rightX, botY);
            p.lineTo(leftX, botY);
            p.close();
            canvas.drawPath(p, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /**
     * Векторная иконка музыки / звука (идеально выверенная геометрическая двойная нота).
     * Построена по канонам Material Design (24x24 сетка) с безупречной соосностью штилей,
     * одинаковыми пропорциями головок нот и ровной соединительной балкой без перекосов.
     */
    private static class MusicIconDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private int color;

        MusicIconDrawable(int color) {
            this.color = color;
            paint.setStyle(Paint.Style.FILL);
        }

        public void setColor(int c) {
            this.color = c;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            if (w <= 0 || h <= 0) return;

            float size = Math.min(w, h);
            float s = size / 24.0f;
            float left = b.left + (w - size) * 0.5f;
            float top = b.top + (h - size) * 0.5f;

            paint.setColor(color);
            path.reset();

            float rx = 3.6f * s;
            float ry = 2.7f * s;
            float stemW = 2.0f * s;
            float beamH = 3.0f * s;

            // Центры головок двух нот (аккуратный естественный подъем второй ноты)
            float cx1 = left + 6.6f * s;
            float cy1 = top + 17.2f * s;
            float cx2 = left + 16.4f * s;
            float cy2 = top + 14.2f * s;

            // Головки первой и второй ноты
            path.addOval(new RectF(cx1 - rx, cy1 - ry, cx1 + rx, cy1 + ry), Path.Direction.CW);
            path.addOval(new RectF(cx2 - rx, cy2 - ry, cx2 + rx, cy2 + ry), Path.Direction.CW);

            // Первый штиль (строго вертикальный, точно по касательной правой грани головки)
            float s1R = cx1 + rx * 0.95f;
            float s1L = s1R - stemW;
            float s1Top = top + 4.5f * s;
            float s1Bot = cy1;

            // Второй штиль (строго вертикальный, повторяет подъем второй ноты)
            float s2R = cx2 + rx * 0.95f;
            float s2L = s2R - stemW;
            float s2Top = top + 1.5f * s;
            float s2Bot = cy2;

            path.addRect(s1L, s1Top, s1R, s1Bot, Path.Direction.CW);
            path.addRect(s2L, s2Top, s2R, s2Bot, Path.Direction.CW);

            // Верхняя соединительная балка (ровный параллелограмм с прямыми вертикальными торцами)
            path.moveTo(s1L, s1Top);
            path.lineTo(s2R, s2Top);
            path.lineTo(s2R, s2Top + beamH);
            path.lineTo(s1L, s1Top + beamH);
            path.close();

            canvas.drawPath(path, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    // ─────────────────────────────────────────── dynamic system theme ─────────

    /**
     * Палитра цветов, динамически адаптирующаяся под цвета системы и тему (светлая/тёмная).
     * На Android 12+ (API 31+) использует системные цвета Monet, на более ранних версиях — тему приложения.
     */
    public static final class SystemTheme {
        public final boolean isNight;
        public final int primary;            // Системный акцентный цвет
        public final int tertiary;           // Динамический цвет Material You (акцент 3)
        public final int onPrimary;          // Контрастный цвет поверх акцента
        public final int surface;            // Фон карточек и диалогов
        public final int onSurface;          // Основной высококонтрастный текст
        public final int onSurfaceVariant;   // Вторичный текст и иконки
        public final int surfaceContainer;   // Фон плашек и неактивных элементов
        public final int primaryContainer;   // Мягкий фон для лучшего качества
        public final int onPrimaryContainer; // Текст на фоне лучшего качества
        public final int outline;            // Тонкая аккуратная обводка (1dp)
        public final int divider;            // Мягкий разделитель

        public SystemTheme(Context ctx) {
            this.isNight = (ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;

            int accent = 0;
            int tert = 0;
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    int resId = isNight ? android.R.color.system_accent1_300 : android.R.color.system_accent1_600;
                    accent = ctx.getColor(resId);
                } catch (Throwable ignored) {}
                try {
                    int tertId = isNight ? android.R.color.system_accent3_300 : android.R.color.system_accent3_600;
                    tert = ctx.getColor(tertId);
                } catch (Throwable ignored) {}
                if (tert == 0) {
                    try {
                        int tertId2 = isNight ? android.R.color.system_accent2_300 : android.R.color.system_accent2_600;
                        tert = ctx.getColor(tertId2);
                    } catch (Throwable ignored) {}
                }
            }
            if (accent == 0) {
                try {
                    TypedValue tv = new TypedValue();
                    if (ctx.getTheme().resolveAttribute(android.R.attr.colorAccent, tv, true)) {
                        accent = tv.data;
                    } else if (ctx.getTheme().resolveAttribute(android.R.attr.colorPrimary, tv, true)) {
                        accent = tv.data;
                    }
                } catch (Throwable ignored) {}
            }
            if (accent == 0) {
                accent = isNight ? 0xFF80D4FF : 0xFF00668B;
            }
            if (tert == 0) {
                tert = isNight ? 0xFFFFB4A9 : 0xFF984061;
            }

            this.primary = accent;
            this.tertiary = tert;
            this.onPrimary = isNight ? 0xFF003547 : 0xFFFFFFFF;

            if (isNight) {
                this.surface = 0xF616171B;
                this.onSurface = 0xFFF0F2F5;
                this.onSurfaceVariant = 0xFF9CA0AB;
                this.surfaceContainer = 0x14FFFFFF;
                this.primaryContainer = Color.argb(45, Color.red(accent), Color.green(accent), Color.blue(accent));
                this.onPrimaryContainer = accent;
                this.outline = 0x22FFFFFF;
                this.divider = 0x12FFFFFF;
            } else {
                this.surface = 0xFCFCFDFE;
                this.onSurface = 0xFF181A1D;
                this.onSurfaceVariant = 0xFF5C6067;
                this.surfaceContainer = 0x0C000000;
                this.primaryContainer = Color.argb(32, Color.red(accent), Color.green(accent), Color.blue(accent));
                this.onPrimaryContainer = accent;
                this.outline = 0x1A000000;
                this.divider = 0x0E000000;
            }
        }
    }

    // ─────────────────────────────────────────── headless folder picker ───────

    /**
     * Headless фрагмент для обработки результата выбора папки через системный проводник
     * (Intent.ACTION_OPEN_DOCUMENT_TREE) без модификации Activity.
     */
    public static class FolderPickerFragment extends android.app.Fragment {
        public static final int REQ_CODE = 4221;
        private VideoQualityPicker plugin;
        private TextView curPathView;

        public void setup(VideoQualityPicker plugin, TextView curPathView) {
            this.plugin = plugin;
            this.curPathView = curPathView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQ_CODE && resultCode == Activity.RESULT_OK && data != null) {
                Uri treeUri = data.getData();
                if (treeUri != null) {
                    VideoQualityPicker p = (plugin != null) ? plugin : sInstance;
                    if (p != null) {
                        p.handlePickedTreeUri(getActivity(), treeUri, curPathView);
                    }
                }
            }
            try {
                if (getFragmentManager() != null) {
                    getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
                }
            } catch (Throwable ignored) {}
        }
    }

    // ─────────────────────────────────────────── haptic feedback ──────────────

    private static boolean isHapticsEnabled() {
        try {
            if (sInstance != null) {
                return sInstance.margyt().prefs().getBoolean("haptics_enabled", true);
            }
        } catch (Throwable ignored) {}
        return true;
    }

    /** Приятный тактильный клик при нажатии на кнопки и выбор вариантов */
    private static void hapticClick(View v) {
        if (!isHapticsEnabled() || v == null) return;
        try {
            v.setHapticFeedbackEnabled(true);
            if (Build.VERSION.SDK_INT >= 21) {
                v.performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                );
            } else {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        } catch (Throwable ignored) {}
        vibratePleasant(v.getContext(), 14, 110);
    }

    /** Легкий мягкий микро-отклик при переключении чипов, вкладок и касании */
    private static void hapticTick(View v) {
        if (!isHapticsEnabled() || v == null) return;
        try {
            v.setHapticFeedbackEnabled(true);
            if (Build.VERSION.SDK_INT >= 27) {
                v.performHapticFeedback(
                        HapticFeedbackConstants.CLOCK_TICK,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                );
            } else if (Build.VERSION.SDK_INT >= 21) {
                v.performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                );
            } else {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } catch (Throwable ignored) {}
        vibratePleasant(v.getContext(), 8, 70);
    }

    /** Отклик при долгом нажатии (открытие настроек) */
    private static void hapticLong(View v) {
        if (!isHapticsEnabled() || v == null) return;
        try {
            v.setHapticFeedbackEnabled(true);
            if (Build.VERSION.SDK_INT >= 21) {
                v.performHapticFeedback(
                        HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                );
            } else {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        } catch (Throwable ignored) {}
        vibratePleasant(v.getContext(), 28, 160);
    }

    /** Приятный двойной микро-пульс при успешном завершении скачивания */
    private static void hapticSuccess(Context ctx) {
        if (!isHapticsEnabled() || ctx == null) return;
        try {
            Vibrator vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 29) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK));
                } else if (Build.VERSION.SDK_INT >= 26) {
                    long[] timings = new long[]{0, 15, 60, 15};
                    int[] amplitudes = new int[]{0, 120, 0, 160};
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
                } else {
                    vib.vibrate(new long[]{0, 20, 60, 25}, -1);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void vibratePleasant(Context ctx, int ms, int amp) {
        if (ctx == null) return;
        try {
            Vibrator vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (vib == null || !vib.hasVibrator()) return;

            if (Build.VERSION.SDK_INT >= 29) {
                if (ms <= 10) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
                } else {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                }
            } else if (Build.VERSION.SDK_INT >= 26) {
                vib.vibrate(VibrationEffect.createOneShot(ms, Math.min(255, Math.max(1, amp))));
            } else {
                vib.vibrate(ms);
            }
        } catch (Throwable ignored) {}
    }

    // ─────────────────────────────────────────── types ───────────────────────

    private static final class Quality {
        final String resolution;    // "1080p Full HD (1080×1920)" или "Звук ролика (MP3)"
        final String detail;        // "4,250 kbps · H.265 (HEVC)" или "Название трека · MP3"
        final List<String> urls;    // Список рабочих зеркал
        final int sortKey;
        final boolean isAudio;

        Quality(String resolution, String detail, List<String> urls, int sortKey) {
            this(resolution, detail, urls, sortKey, false);
        }

        Quality(String resolution, String detail, List<String> urls, int sortKey, boolean isAudio) {
            this.resolution = resolution;
            this.detail     = detail;
            this.urls       = urls;
            this.sortKey    = sortKey;
            this.isAudio    = isAudio;
        }
    }

    // ─────────────────────────────────────────── state ───────────────────────

    private static final String BUTTON_TAG = "vqp_floating_btn";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile VideoQualityPicker sInstance;

    private volatile boolean isDownloadCancelled = false;
    private volatile View activeProgressOverlay = null;
    private volatile TextView activeProgressPercentView = null;
    private volatile ProgressBar activeProgressBar = null;
    private volatile Activity activeDownloadActivity = null;

    private final Map<String, List<Quality>> qualityCache = new ConcurrentHashMap<>();
    private final Map<String, Object> awemeCache = new ConcurrentHashMap<>();
    private final List<Object> recentAwemes = new CopyOnWriteArrayList<>();

    private volatile String lastFeedId = null;
    private volatile String intentId   = null;

    private volatile String lastRenderedUid = null;
    private volatile String lastRenderedName = null;
    private volatile long lastRenderedTime = 0;

    private static final String[] EXCLUDED_ACTIVITIES = {
            "Setting",
            "SettingsActivity",
            "PreferencesActivity",
            "Record",
            "Shoot",
            "Publish",
            "LiveRoom",
            "LiveBroadcast",
            "LivePlay",
            "Login",
            "Authorize",
    };

    private static boolean isExcludedActivity(String name) {
        if (name == null) return true;
        for (String ex : EXCLUDED_ACTIVITIES) {
            if (name.contains(ex)) return true;
        }
        return false;
    }

    // ─────────────────────────────────────────── hooks ───────────────────────

    @Override
    public void onStart(final Context context) {
        sInstance = this;
        try {
            margyt().addSettingsRow("Выбор качества видео", "Размер, прозрачность, путь сохранения и позиция", () -> {
                Activity act = margyt().screen();
                if (act == null && context instanceof Activity) {
                    act = (Activity) context;
                }
                if (act != null) {
                    final Activity finalAct = act;
                    MAIN.post(() -> openSettingsDialog(finalAct));
                }
            });
        } catch (Throwable t) {
            margyt().log("vqp onStart settings: " + t);
        }
    }

    @Override
    public List onFeed(List posts) {
        if (posts == null) return posts;
        for (Object post : posts) {
            try {
                processPost(post);
            } catch (Throwable t) {
                margyt().log("vqp feed: " + t);
            }
        }
        return posts;
    }

    @Override
    public String onName(String uid, String name) {
        if (name != null && !name.trim().isEmpty()) {
            lastRenderedUid = uid;
            lastRenderedName = name.trim();
            lastRenderedTime = System.currentTimeMillis();
        }
        return name;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        String name = activity.getClass().getName();
        logActivityName(name);

        if (isExcludedActivity(name)) {
            detachFloatingButton(activity);
            return;
        }

        String id = idFromIntent(activity);
        if (id != null) intentId = id;

        attachFloatingButton(activity);
    }

    @Override
    public void onScreen(Activity activity, String name) {
        if (activity != null && !activity.isFinishing()) {
            if (!isExcludedActivity(activity.getClass().getName())) {
                attachFloatingButton(activity);
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        detachFloatingButton(activity);
    }

    // ─────────────────────────────────────────── floating button ─────────────

    private void attachFloatingButton(final Activity activity) {
        MAIN.post(() -> {
            try {
                if (activity == null || activity.isFinishing()) return;
                if (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) return;

                final ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                if (decor.findViewWithTag(BUTTON_TAG) != null) return;

                final TextView btn = new TextView(activity);
                btn.setTag(BUTTON_TAG);
                btn.setTextColor(0xFFFFFFFF);
                btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                btn.setGravity(Gravity.CENTER);

                // Применяем сохраненную прозрачность и размер
                btn.setAlpha(getSavedAlpha());
                applyButtonSize(btn, activity, getSavedSize());

                if (Build.VERSION.SDK_INT >= 21) {
                    btn.setElevation(dp(activity, 6));
                }

                int savedX = -1;
                int savedY = -1;
                try {
                    savedX = margyt().prefs().getInt("btn_x", -1);
                    savedY = margyt().prefs().getInt("btn_y", -1);
                } catch (Throwable ignored) {}

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                if (savedX >= 0 && savedY >= 0) {
                    lp.gravity = Gravity.TOP | Gravity.START;
                    lp.leftMargin = savedX;
                    lp.topMargin = savedY;
                } else {
                    lp.gravity = Gravity.TOP | Gravity.END;
                    lp.topMargin = dp(activity, 90);
                    lp.rightMargin = dp(activity, 12);
                }

                btn.setOnTouchListener(new View.OnTouchListener() {
                    private float downX, downY;
                    private int initLeft, initTop;
                    private boolean isDragging = false;
                    private boolean isLongPressed = false;
                    private static final int SLOP = 12;

                    private final Runnable longPressRunnable = () -> {
                        if (!isDragging) {
                            isLongPressed = true;
                            try {
                                btn.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            } catch (Throwable ignored) {}
                            openSettingsDialog(activity);
                        }
                    };

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                downX = event.getRawX();
                                downY = event.getRawY();
                                initLeft = (int) v.getX();
                                initTop = (int) v.getY();
                                isDragging = false;
                                isLongPressed = false;
                                v.setAlpha(1.0f);
                                hapticTick(v);
                                MAIN.postDelayed(longPressRunnable, 550);
                                return true;

                            case MotionEvent.ACTION_MOVE:
                                float dx = event.getRawX() - downX;
                                float dy = event.getRawY() - downY;
                                if (!isDragging && (Math.abs(dx) > SLOP || Math.abs(dy) > SLOP)) {
                                    isDragging = true;
                                    MAIN.removeCallbacks(longPressRunnable);
                                }
                                if (isDragging) {
                                    FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) v.getLayoutParams();
                                    p.gravity = Gravity.TOP | Gravity.START;
                                    p.leftMargin = Math.max(0, (int) (initLeft + dx));
                                    p.topMargin = Math.max(0, (int) (initTop + dy));
                                    p.rightMargin = 0;
                                    p.bottomMargin = 0;
                                    v.setLayoutParams(p);
                                }
                                return true;

                            case MotionEvent.ACTION_CANCEL:
                                MAIN.removeCallbacks(longPressRunnable);
                                v.setAlpha(getSavedAlpha());
                                return true;

                            case MotionEvent.ACTION_UP:
                                MAIN.removeCallbacks(longPressRunnable);
                                v.setAlpha(getSavedAlpha());
                                if (!isDragging && !isLongPressed) {
                                    hapticClick(v);
                                    v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(70)
                                            .withEndAction(() -> {
                                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
                                                onButtonTapped(activity);
                                            }).start();
                                } else if (isDragging) {
                                    hapticTick(v);
                                    FrameLayout.LayoutParams endP = (FrameLayout.LayoutParams) v.getLayoutParams();
                                    try {
                                        margyt().prefs().edit()
                                                .putInt("btn_x", endP.leftMargin)
                                                .putInt("btn_y", endP.topMargin)
                                                .apply();
                                    } catch (Throwable ignored) {}
                                }
                                return true;
                        }
                        return false;
                    }
                });

                decor.addView(btn, lp);

                // Отслеживание вкладок: скрытие кнопки в профиле и входящих
                final ViewTreeObserver vto = decor.getViewTreeObserver();
                if (vto != null && vto.isAlive()) {
                    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            try {
                                if (activity.isFinishing()) {
                                    if (Build.VERSION.SDK_INT >= 16) {
                                        decor.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    }
                                    return;
                                }
                                boolean inFeed = isUserInFeed(activity);
                                int targetVis = inFeed ? View.VISIBLE : View.GONE;
                                if (btn.getVisibility() != targetVis) {
                                    btn.setVisibility(targetVis);
                                }
                            } catch (Throwable ignored) {}
                        }
                    });
                }

                btn.setVisibility(isUserInFeed(activity) ? View.VISIBLE : View.GONE);

            } catch (Throwable t) {
                margyt().log("vqp attach button: " + t);
            }
        });
    }

    private void detachFloatingButton(Activity activity) {
        MAIN.post(() -> {
            try {
                if (activity == null) return;
                ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                View btn = decor.findViewWithTag(BUTTON_TAG);
                if (btn != null) {
                    decor.removeView(btn);
                }
            } catch (Throwable ignored) {}
        });
    }

    // ─────────────────────────────────────────── feed / profile detection ────

    private boolean isUserInFeed(Activity activity) {
        if (activity == null || activity.isFinishing()) return false;
        String actName = activity.getClass().getName();

        if (isExcludedActivity(actName)) {
            return false;
        }

        // Если это специализированная активность поиска/видео/плеера (не MainActivity) — всегда показываем
        if (!actName.contains("MainActivity")) {
            return true;
        }

        // 1. Проверяем, есть ли на экране активный полноразмерный видеоплеер
        // В ленте и при воспроизведении видео в поиске TextureView ВСЕГДА присутствует и активен!
        try {
            View decor = activity.getWindow().getDecorView();
            int screenH = activity.getResources().getDisplayMetrics().heightPixels;
            if (hasActiveVideoPlayer(decor, screenH)) {
                return true;
            }
        } catch (Throwable ignored) {}

        // 2. Если видеоплеер не обнаружен, проверяем вкладки навигации (Профиль / Входящие)
        try {
            View decor = activity.getWindow().getDecorView();
            int h = activity.getResources().getDisplayMetrics().heightPixels;
            int tab = checkBottomNavTab(decor, h);
            if (tab == 1) return true;   // Главная / Home
            if (tab == -1) return false; // Профиль / Входящие без воспроизведения
        } catch (Throwable ignored) {}

        // 3. Проверяем видимые фрагменты профиля и чатов
        try {
            Method getFM = activity.getClass().getMethod("getSupportFragmentManager");
            Object fm = getFM.invoke(activity);
            Method getFrags = fm.getClass().getMethod("getFragments");
            List<?> frags = (List<?>) getFrags.invoke(fm);
            if (frags != null) {
                for (Object f : frags) {
                    if (f == null) continue;
                    boolean isVis = false;
                    try {
                        Method mv = f.getClass().getMethod("isVisible");
                        isVis = (Boolean) mv.invoke(f);
                    } catch (Throwable ignored) {
                        try {
                            Method mres = f.getClass().getMethod("isResumed");
                            isVis = (Boolean) mres.invoke(f);
                        } catch (Throwable ignored2) {}
                    }

                    if (isVis) {
                        String simple = f.getClass().getSimpleName();
                        if (simple.contains("UserProfile") || simple.contains("MyProfile")
                                || simple.contains("ProfilePage") || simple.contains("NoticeFragment")
                                || simple.contains("InboxFragment") || simple.contains("ChatList")) {
                            return false;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return true;
    }

    private boolean hasActiveVideoPlayer(View v, int screenH) {
        if (v == null || v.getVisibility() != View.VISIBLE) return false;

        if (v instanceof android.view.TextureView || v instanceof android.view.SurfaceView) {
            int h = v.getHeight();
            int w = v.getWidth();
            if (h > screenH * 0.30f || (w > 300 && h > 400)) {
                return true;
            }
        }

        String cls = v.getClass().getName();
        if (cls.contains("TextureView") || cls.contains("SurfaceView") || cls.contains("VideoView")) {
            if (v.getHeight() > screenH * 0.30f) {
                return true;
            }
        }

        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            int count = g.getChildCount();
            for (int i = 0; i < count; i++) {
                if (hasActiveVideoPlayer(g.getChildAt(i), screenH)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int checkBottomNavTab(View v, int screenHeight) {
        if (v == null || v.getVisibility() != View.VISIBLE) return 0;

        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        if (loc[1] > screenHeight * 0.85) {
            if (v.isSelected()) {
                CharSequence cd = v.getContentDescription();
                String desc = cd != null ? cd.toString().trim().toLowerCase() : "";
                if (v instanceof TextView) {
                    desc = ((TextView) v).getText().toString().trim().toLowerCase();
                }

                if (desc.equals("главная") || desc.equals("home") || desc.equals("лента") || desc.equals("feed")) {
                    return 1;
                }
                if (desc.equals("профиль") || desc.equals("profile")
                        || desc.equals("входящие") || desc.equals("inbox")
                        || desc.equals("сообщения") || desc.equals("друзья") || desc.equals("friends")) {
                    return -1;
                }
            }
        }

        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                int res = checkBottomNavTab(g.getChildAt(i), screenHeight);
                if (res != 0) return res;
            }
        }
        return 0;
    }

    // ─────────────────────────────────────────── button handler ──────────────

    private void onButtonTapped(Activity activity) {
        margyt().away("resolve_video", () -> {
            try {
                Activity liveAct = getLiveActivity(activity);
                if (liveAct == null) return;

                Object targetAweme = findCurrentActiveAweme(liveAct);

                if (targetAweme != null) {
                    String id = getAwemeId(targetAweme);
                    String author = getAuthorName(targetAweme);
                    String desc = callStr(targetAweme, "getDesc");
                    boolean isPhoto = isImagePost(targetAweme);
                    int imgCount = getImageCount(targetAweme);

                    List<Quality> qualities = extractQualitiesFromAweme(targetAweme);
                    if (!qualities.isEmpty()) {
                        margyt().log("vqp: активный " + (isPhoto ? "фотопост" : "ролик") + " [" + id + "] @" + author + " — качеств: " + qualities.size());
                        showQualityDialog(liveAct, qualities, author, desc, isPhoto, imgCount);
                        return;
                    }
                }

                safeAlert(liveAct, "Качество не найдено",
                        "Не удалось определить ролик.\nСмахните его вверх/вниз и нажмите кнопку снова.");
            } catch (Throwable t) {
                margyt().log("vqp onButtonTapped error: " + t);
            }
        });
    }

    /**
     * Высокоточный мгновенный поиск активного видео в ленте TikTok.
     * 1. Инспекция View-адаптера (ViewPager / RecyclerView) и текущей позиции плеера.
     * 2. Контекстный скоринг видимых на экране текстов и хука onName() по кэшу Aweme.
     */
    private Object findCurrentActiveAweme(Activity activity) {
        if (activity == null) return null;

        // 1. Поиск прямого объекта Aweme из ViewPager / RecyclerView плеера
        try {
            Object aweme = findAwemeFromViewHierarchy(activity.getWindow().getDecorView());
            if (aweme != null) {
                margyt().log("vqp: ролик найден через View-контейнер: @" + getAuthorName(aweme));
                return aweme;
            }
        } catch (Throwable ignored) {}

        // 2. Интеллектуальный скоринг по видимым текстам на экране и хуку onName()
        try {
            Object aweme = scoreAndMatchActiveAweme(activity);
            if (aweme != null) {
                margyt().log("vqp: ролик найден через контекстный скоринг: @" + getAuthorName(aweme));
                return aweme;
            }
        } catch (Throwable ignored) {}

        // 3. Если отдельное окно плеера (AwemeDetailActivity)
        if (intentId != null && !activity.getClass().getName().contains("MainActivity")) {
            Object aweme = awemeCache.get(intentId);
            if (aweme != null) return aweme;
        }

        return null;
    }

    private Object findAwemeFromViewHierarchy(View root) {
        if (root == null) return null;
        return inspectViewForAweme(root, 0);
    }

    private Object inspectViewForAweme(View v, int depth) {
        if (v == null || depth > 30 || v.getVisibility() != View.VISIBLE) return null;

        // Проверяем, поддерживает ли данный ViewGroup выбор текущего элемента (ViewPager / ViewPager2)
        try {
            Method mCur = v.getClass().getMethod("getCurrentItem");
            int pos = (Integer) mCur.invoke(v);
            Method mAdap = v.getClass().getMethod("getAdapter");
            Object adapter = mAdap.invoke(v);
            if (adapter != null) {
                // Попытка 1: getItem(pos)
                try {
                    Method mGet = adapter.getClass().getMethod("getItem", int.class);
                    Object item = mGet.invoke(adapter, pos);
                    if (isAweme(item)) return item;
                    if (item != null) {
                        for (String m : new String[]{"getAweme", "getCurAweme", "getCurrentAweme"}) {
                            Object a = call(item, m);
                            if (isAweme(a)) return a;
                        }
                    }
                } catch (Throwable ignored) {}

                // Попытка 2: списки элементов в адаптере
                for (String m : new String[]{"getItems", "getData", "getAwemeList", "getFeedList"}) {
                    Object list = call(adapter, m);
                    if (list instanceof List) {
                        List<?> l = (List<?>) list;
                        if (pos >= 0 && pos < l.size()) {
                            Object a = l.get(pos);
                            if (isAweme(a)) return a;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Проверяем тег текущего View
        Object tag = v.getTag();
        if (isAweme(tag)) {
            return tag;
        }

        // В RecyclerView (активно используется в поиске) в LayoutParams дочернего элемента хранится ViewHolder
        try {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp != null) {
                for (Field f : allFields(lp.getClass())) {
                    String fn = f.getName().toLowerCase();
                    if (fn.contains("viewholder")) {
                        f.setAccessible(true);
                        Object holder = f.get(lp);
                        if (holder != null) {
                            for (Field hf : allFields(holder.getClass())) {
                                hf.setAccessible(true);
                                Object hVal = hf.get(holder);
                                if (isAweme(hVal)) return hVal;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Рекурсивно обходим дочерние элементы
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                Object res = inspectViewForAweme(g.getChildAt(i), depth + 1);
                if (res != null) return res;
            }
        }
        return null;
    }

    private Object scoreAndMatchActiveAweme(Activity activity) {
        if (recentAwemes.isEmpty()) return null;

        int screenH = activity.getResources().getDisplayMetrics().heightPixels;
        List<String> videoAreaTexts = new ArrayList<>();
        collectVisibleVideoTexts(activity.getWindow().getDecorView(), videoAreaTexts, screenH, 0);

        Object bestAweme = null;
        int bestScore = 0;

        long now = System.currentTimeMillis();
        boolean hasRecentRendered = (now - lastRenderedTime < 45000) && (lastRenderedName != null || lastRenderedUid != null);

        for (int i = recentAwemes.size() - 1; i >= 0; i--) {
            Object aweme = recentAwemes.get(i);
            int score = calculateAwemeScore(aweme, videoAreaTexts, hasRecentRendered);
            if (score > bestScore) {
                bestScore = score;
                bestAweme = aweme;
            }
        }

        // Порог уверенности: совпал хотя бы никнейм автора или ключевые слова описания
        if (bestScore >= 30) {
            return bestAweme;
        }
        return null;
    }

    private void collectVisibleVideoTexts(View v, List<String> out, int screenH, int depth) {
        if (v == null || depth > 35 || v.getVisibility() != View.VISIBLE) return;

        if (v instanceof TextView) {
            CharSequence cs = ((TextView) v).getText();
            if (cs != null) {
                String text = cs.toString().trim();
                if (text.length() >= 2) {
                    int[] loc = new int[2];
                    v.getLocationOnScreen(loc);
                    int y = loc[1];
                    // Фильтруем: только тексты в зоне видео (от 10% до 88% экрана),
                    // исключая верхний статус-бар/вкладки и нижнюю панель навигации («Профиль», «Входящие»)
                    if (y >= screenH * 0.10 && y <= screenH * 0.88) {
                        out.add(text);
                    }
                }
            }
        }

        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                collectVisibleVideoTexts(g.getChildAt(i), out, screenH, depth + 1);
            }
        }
    }

    private int calculateAwemeScore(Object aweme, List<String> screenTexts, boolean hasRecentRendered) {
        int score = 0;
        String nick = getAuthorNick(aweme);
        String uid = getAuthorUid(aweme);
        String desc = callStr(aweme, "getDesc");

        // Исключаем фоновые совпадения с ником "fodis3"
        boolean isOwnNick = (nick != null && nick.equalsIgnoreCase("fodis3"));

        // 1. Проверка по хуку onName()
        if (hasRecentRendered && !isOwnNick) {
            if (lastRenderedName != null && nick != null && lastRenderedName.equalsIgnoreCase(nick)) {
                score += 150;
            }
            if (lastRenderedUid != null && uid != null && lastRenderedUid.equalsIgnoreCase(uid)) {
                score += 150;
            }
        }

        // 2. Проверка по видимым на экране строкам
        for (String line : screenTexts) {
            if (line.isEmpty()) continue;
            String lineLower = line.toLowerCase();

            // Совпадение ника автора
            if (nick != null && !nick.isEmpty() && !isOwnNick) {
                String nickLower = nick.toLowerCase();
                if (line.equalsIgnoreCase(nick)) {
                    score += 120;
                } else if (lineLower.contains(nickLower)) {
                    score += 90;
                }
            }

            // Совпадение уникального логина (uniqueId)
            if (uid != null && !uid.isEmpty()) {
                String uidLower = uid.toLowerCase();
                if (lineLower.contains(uidLower)) {
                    score += 90;
                }
            }

            // Совпадение описания целиком
            if (desc != null && desc.length() >= 6) {
                String descClean = desc.replace("\n", " ").trim().toLowerCase();
                if (lineLower.contains(descClean) || descClean.contains(lineLower)) {
                    score += 100;
                }
            }
        }

        // 3. Совпадение по отдельным словам описания и хэштегам
        if (desc != null && !desc.isEmpty()) {
            String[] words = desc.split("[\\s,\\.!\\?#]+");
            for (String w : words) {
                if (w.length() >= 4) {
                    String wLower = w.toLowerCase();
                    for (String line : screenTexts) {
                        if (line.toLowerCase().contains(wLower)) {
                            score += 30;
                            break;
                        }
                    }
                }
            }
        }

        // 4. Совпадение по названию музыки
        try {
            Object music = call(aweme, "getMusic");
            if (music != null) {
                String mTitle = callStr(music, "getTitle");
                if (mTitle != null && mTitle.length() >= 4) {
                    String mLower = mTitle.toLowerCase();
                    for (String line : screenTexts) {
                        if (line.toLowerCase().contains(mLower)) {
                            score += 40;
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return score;
    }

    // ─────────────────────────────────────────── custom dialog (Minimalistic System-Colored) ─────────

    /**
     * Минималистичное диалоговое окно выбора качества, динамически подстраивающееся под системные цвета и тему.
     */
    private void showQualityDialog(final Activity activity, final List<Quality> qualities,
                                   final String author, final String descSnippet) {
        showQualityDialog(activity, qualities, author, descSnippet, false, 0);
    }

    private void showQualityDialog(final Activity activity, final List<Quality> qualities,
                                   final String author, final String descSnippet,
                                   final boolean isPhotoPost, final int imgCount) {
        MAIN.post(() -> {
            try {
                Activity act = getLiveActivity(activity);
                if (act == null || act.isFinishing()) return;

                final Dialog dialog = new Dialog(act);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Window win = dialog.getWindow();
                if (win != null) {
                    win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    win.setDimAmount(0.55f);
                }

                final SystemTheme theme = new SystemTheme(act);

                // Корневая карточка диалога — минималистичный стиль с системными цветами
                LinearLayout card = new LinearLayout(act);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER_HORIZONTAL);
                int padH = dp(act, 20);
                int padV = dp(act, 18);
                card.setPadding(padH, padV, padH, dp(act, 16));

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(theme.surface);
                cardBg.setCornerRadius(dp(act, 22));
                cardBg.setStroke(dp(act, 1), theme.outline);
                card.setBackground(cardBg);

                // Шапка диалога: векторная иконка + заголовок + кнопка настроек (шестерёнка)
                LinearLayout header = new LinearLayout(act);
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                header.setLayoutParams(headerLp);

                // Векторная иконка скачивания или музыки
                ImageView headIcon = new ImageView(act);
                Drawable headDraw = isPhotoPost
                        ? new MusicIconDrawable(theme.primary)
                        : new DownloadIconDrawable(theme.primary);
                int headIconSize = dp(act, 20);
                headDraw.setBounds(0, 0, headIconSize, headIconSize);
                headIcon.setImageDrawable(headDraw);
                LinearLayout.LayoutParams headIconLp = new LinearLayout.LayoutParams(headIconSize, headIconSize);
                headIconLp.rightMargin = dp(act, 8);
                headIcon.setLayoutParams(headIconLp);
                header.addView(headIcon);

                // Текст заголовка
                TextView title = new TextView(act);
                title.setText(isPhotoPost ? "Звук фотопоста" : "Скачать видео");
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.5f);
                title.setTextColor(theme.onSurface);
                title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                title.setLayoutParams(titleLp);
                header.addView(title);

                // Векторная кнопка перехода в настройки плагина (шестерёнка)
                ImageView gearBtn = new ImageView(act);
                GearIconDrawable gearDraw = new GearIconDrawable(theme.onSurfaceVariant);
                int gearIconSize = dp(act, 18);
                gearDraw.setBounds(0, 0, gearIconSize, gearIconSize);
                gearBtn.setImageDrawable(gearDraw);
                int gearBtnSize = dp(act, 32);
                LinearLayout.LayoutParams gearLp = new LinearLayout.LayoutParams(gearBtnSize, gearBtnSize);
                gearBtn.setLayoutParams(gearLp);
                gearBtn.setPadding(dp(act, 6), dp(act, 6), dp(act, 6), dp(act, 6));

                GradientDrawable gearBg = new GradientDrawable();
                gearBg.setShape(GradientDrawable.OVAL);
                gearBg.setColor(theme.surfaceContainer);
                gearBg.setStroke(dp(act, 1), theme.divider);
                gearBtn.setBackground(gearBg);

                gearBtn.setOnClickListener(v -> {
                    hapticClick(gearBtn);
                    dialog.dismiss();
                    openSettingsDialog(act);
                });
                header.addView(gearBtn);

                card.addView(header);

                // Подзаголовок: автор и превью текста ролика в минималистичном бейдже
                if (author != null && !author.isEmpty()) {
                    TextView sub = new TextView(act);
                    StringBuilder info = new StringBuilder("@").append(author);
                    if (isPhotoPost && imgCount > 0) {
                        info.append(" • ").append(imgCount).append(" фото");
                    }
                    if (descSnippet != null && !descSnippet.trim().isEmpty()) {
                        String cleanD = descSnippet.replace("\n", " ").trim();
                        if (cleanD.length() > 26) cleanD = cleanD.substring(0, 26) + "...";
                        info.append(" • ").append(cleanD);
                    }
                    sub.setText(info.toString());
                    sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                    sub.setTextColor(theme.primary);
                    sub.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    sub.setGravity(Gravity.CENTER);
                    sub.setMaxLines(1);
                    sub.setEllipsize(TextUtils.TruncateAt.END);
                    sub.setPadding(dp(act, 8), dp(act, 3), dp(act, 8), dp(act, 3));

                    GradientDrawable subBg = new GradientDrawable();
                    subBg.setColor(theme.primaryContainer);
                    subBg.setCornerRadius(dp(act, 10));
                    subBg.setStroke(dp(act, 1), theme.outline);
                    sub.setBackground(subBg);

                    LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    subLp.topMargin = dp(act, 6);
                    subLp.bottomMargin = dp(act, 12);
                    sub.setLayoutParams(subLp);

                    card.addView(sub);
                } else {
                    View space = new View(act);
                    card.addView(space, new LinearLayout.LayoutParams(1, dp(act, 10)));
                }

                // Список вариантов качеств
                LinearLayout listContainer = new LinearLayout(act);
                listContainer.setOrientation(LinearLayout.VERTICAL);

                if (!qualities.isEmpty()) {
                    if (isPhotoPost) {
                        // Для фотопоста: выводим скачивание звука
                        for (final Quality q : qualities) {
                            listContainer.addView(createQualityRow(act, dialog, q, true, author, theme));
                        }
                    } else {
                        // 1. Для обычного видео: по умолчанию самый верхний пункт — лучшее качество
                        final Quality bestQ = qualities.get(0);
                        listContainer.addView(createQualityRow(act, dialog, bestQ, true, author, theme));

                    // 2. Всё остальное скрывается в раскрывающемся списке
                    if (qualities.size() > 1) {
                        final List<Quality> otherQualities = qualities.subList(1, qualities.size());

                        final LinearLayout expandableContainer = new LinearLayout(act);
                        expandableContainer.setOrientation(LinearLayout.VERTICAL);
                        expandableContainer.setVisibility(View.GONE);

                        for (final Quality q : otherQualities) {
                            expandableContainer.addView(createQualityRow(act, dialog, q, false, author, theme));
                        }

                        final TextView toggleBtn = new TextView(act);
                        toggleBtn.setText("Другие варианты качества (" + otherQualities.size() + ")");
                        toggleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                        toggleBtn.setTextColor(theme.primary);
                        toggleBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                        toggleBtn.setGravity(Gravity.CENTER);
                        toggleBtn.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));

                        final ChevronIconDrawable chevron = new ChevronIconDrawable(theme.primary, false);
                        int chSize = dp(act, 11);
                        chevron.setBounds(0, 0, chSize, chSize);
                        toggleBtn.setCompoundDrawables(null, null, chevron, null);
                        toggleBtn.setCompoundDrawablePadding(dp(act, 8));

                        GradientDrawable toggleBg = new GradientDrawable();
                        toggleBg.setColor(theme.surfaceContainer);
                        toggleBg.setCornerRadius(dp(act, 14));
                        toggleBg.setStroke(dp(act, 1), theme.divider);
                        toggleBtn.setBackground(toggleBg);

                        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        toggleLp.topMargin = dp(act, 2);
                        toggleLp.bottomMargin = dp(act, 8);
                        toggleBtn.setLayoutParams(toggleLp);

                        toggleBtn.setOnClickListener(v -> {
                            hapticTick(toggleBtn);
                            boolean isShown = (expandableContainer.getVisibility() == View.VISIBLE);
                            if (isShown) {
                                expandableContainer.setVisibility(View.GONE);
                                chevron.setExpanded(false);
                                toggleBtn.setText("Другие варианты (" + otherQualities.size() + ")");
                            } else {
                                expandableContainer.setVisibility(View.VISIBLE);
                                chevron.setExpanded(true);
                                toggleBtn.setText("Скрыть другие варианты");
                            }
                        });

                            listContainer.addView(toggleBtn);
                            listContainer.addView(expandableContainer);
                        }
                    }
                }

                ScrollView scroller = new ScrollView(act) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        int maxH = (int) (act.getResources().getDisplayMetrics().heightPixels * 0.65f);
                        heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST);
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    }
                };
                scroller.setVerticalScrollBarEnabled(false);
                scroller.addView(listContainer);

                LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                scroller.setLayoutParams(scrollLp);
                card.addView(scroller);

                // Кнопка отмены — минималистичная
                TextView cancelBtn = new TextView(act);
                cancelBtn.setText("Отмена");
                cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                cancelBtn.setTextColor(theme.onSurfaceVariant);
                cancelBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                cancelBtn.setGravity(Gravity.CENTER);
                cancelBtn.setPadding(0, dp(act, 10), 0, dp(act, 10));

                GradientDrawable cancelBg = new GradientDrawable();
                cancelBg.setColor(theme.surfaceContainer);
                cancelBg.setCornerRadius(dp(act, 14));
                cancelBg.setStroke(dp(act, 1), theme.divider);
                cancelBtn.setBackground(cancelBg);

                LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cancelLp.topMargin = dp(act, 4);
                cancelBtn.setLayoutParams(cancelLp);

                cancelBtn.setOnClickListener(v -> {
                    hapticTick(cancelBtn);
                    dialog.dismiss();
                });
                card.addView(cancelBtn);

                dialog.setContentView(card);

                int displayW = act.getResources().getDisplayMetrics().widthPixels;
                int cardW = Math.min(dp(act, 350), (int) (displayW * 0.90f));
                dialog.getWindow().setLayout(cardW, ViewGroup.LayoutParams.WRAP_CONTENT);

                dialog.show();
            } catch (Throwable t) {
                margyt().log("vqp showQualityDialog: " + t);
            }
        });
    }

    private View createQualityRow(final Activity act, final Dialog dialog, final Quality q,
                                  boolean isBest, final String author, final SystemTheme theme) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(act, 12), dp(act, 10), dp(act, 12), dp(act, 10));

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowLp.bottomMargin = dp(act, 6);
        row.setLayoutParams(rowLp);

        GradientDrawable rowBg = new GradientDrawable();
        if (isBest) {
            rowBg.setColor(theme.primaryContainer);
            rowBg.setStroke(dp(act, 1), theme.primary);
            rowBg.setCornerRadius(dp(act, 14));
        } else {
            rowBg.setColor(theme.surfaceContainer);
            rowBg.setStroke(dp(act, 1), theme.outline);
            rowBg.setCornerRadius(dp(act, 14));
        }
        row.setBackground(rowBg);

        LinearLayout textCol = new LinearLayout(act);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
        );
        textCol.setLayoutParams(colLp);

        if (q.isAudio) {
            TextView audioBadge = new TextView(act);
            audioBadge.setText(isBest ? "МУЗЫКА РОЛИКА (MP3)" : "АУДИОДОРОЖКА");
            audioBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            audioBadge.setTextColor(theme.primary);
            audioBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            audioBadge.setPadding(dp(act, 5), dp(act, 1), dp(act, 5), dp(act, 1));

            MusicIconDrawable musicIcon = new MusicIconDrawable(theme.primary);
            int mSize = dp(act, 9);
            musicIcon.setBounds(0, 0, mSize, mSize);
            audioBadge.setCompoundDrawables(musicIcon, null, null, null);
            audioBadge.setCompoundDrawablePadding(dp(act, 4));

            GradientDrawable aBg = new GradientDrawable();
            aBg.setColor(theme.primaryContainer);
            aBg.setCornerRadius(dp(act, 5));
            audioBadge.setBackground(aBg);

            LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            aLp.bottomMargin = dp(act, 2);
            audioBadge.setLayoutParams(aLp);
            textCol.addView(audioBadge);
        } else if (q.resolution.contains("Слайд-шоу") || q.detail.contains("слайд-шоу")) {
            TextView slideBadge = new TextView(act);
            slideBadge.setText("СЛАЙД-ШОУ (MP4)");
            slideBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            slideBadge.setTextColor(theme.primary);
            slideBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            slideBadge.setPadding(dp(act, 5), dp(act, 1), dp(act, 5), dp(act, 1));

            DownloadIconDrawable dlIcon = new DownloadIconDrawable(theme.primary);
            int dSize = dp(act, 9);
            dlIcon.setBounds(0, 0, dSize, dSize);
            slideBadge.setCompoundDrawables(dlIcon, null, null, null);
            slideBadge.setCompoundDrawablePadding(dp(act, 4));

            GradientDrawable sBg = new GradientDrawable();
            sBg.setColor(theme.primaryContainer);
            sBg.setCornerRadius(dp(act, 5));
            slideBadge.setBackground(sBg);

            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sLp.bottomMargin = dp(act, 2);
            slideBadge.setLayoutParams(sLp);
            textCol.addView(slideBadge);
        } else if (isBest) {
            TextView badge = new TextView(act);
            badge.setText("ЛУЧШЕЕ КАЧЕСТВО");
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            badge.setTextColor(theme.primary);
            badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            badge.setPadding(dp(act, 5), dp(act, 1), dp(act, 5), dp(act, 1));

            StarIconDrawable star = new StarIconDrawable(theme.primary);
            int starSize = dp(act, 9);
            star.setBounds(0, 0, starSize, starSize);
            badge.setCompoundDrawables(star, null, null, null);
            badge.setCompoundDrawablePadding(dp(act, 4));

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(theme.primaryContainer);
            badgeBg.setCornerRadius(dp(act, 5));
            badge.setBackground(badgeBg);

            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.bottomMargin = dp(act, 2);
            badge.setLayoutParams(bLp);
            textCol.addView(badge);
        }

        TextView resView = new TextView(act);
        resView.setText(q.resolution);
        resView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        resView.setTextColor(theme.onSurface);
        resView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textCol.addView(resView);

        TextView detailView = new TextView(act);
        detailView.setText(q.detail);
        detailView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        detailView.setTextColor(theme.onSurfaceVariant);
        detailView.setPadding(0, dp(act, 1), 0, 0);
        textCol.addView(detailView);

        row.addView(textCol);

        // Векторная кнопка скачивания / аудио
        ImageView arrow = new ImageView(act);
        boolean isAccent = isBest || q.resolution.contains("Слайд-шоу");
        Drawable arrowDraw = q.isAudio
                ? new MusicIconDrawable(theme.primary)
                : new DownloadIconDrawable(isAccent ? theme.primary : theme.onSurfaceVariant);
        int arrowIconSize = dp(act, 15);
        arrowDraw.setBounds(0, 0, arrowIconSize, arrowIconSize);
        arrow.setImageDrawable(arrowDraw);

        int iconSize = dp(act, 30);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        arrow.setLayoutParams(iconLp);
        arrow.setPadding(dp(act, 7), dp(act, 7), dp(act, 7), dp(act, 7));

        GradientDrawable arrowBg = new GradientDrawable();
        arrowBg.setShape(GradientDrawable.OVAL);
        arrowBg.setColor(theme.surfaceContainer);
        arrowBg.setStroke(dp(act, 1), theme.divider);
        arrow.setBackground(arrowBg);
        row.addView(arrow);

        row.setOnClickListener(v -> {
            hapticClick(row);
            dialog.dismiss();
            startStreamingDownload(act, q, author);
        });

        row.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                hapticTick(row);
                row.setAlpha(0.6f);
            } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                row.setAlpha(1.0f);
            }
            return false;
        });

        return row;
    }

    // ─────────────────────────────────────────── streaming download & overlay ─

    private void showDownloadOverlay(final Activity activity, final String label, final boolean isAudio) {
        MAIN.post(() -> {
            try {
                Activity act = getLiveActivity(activity);
                if (act == null || act.isFinishing()) return;

                activeDownloadActivity = act;
                hideDownloadOverlayInternal(act);

                final ViewGroup decor = (ViewGroup) act.getWindow().getDecorView();
                SystemTheme theme = new SystemTheme(act);

                LinearLayout overlay = new LinearLayout(act);
                overlay.setOrientation(LinearLayout.HORIZONTAL);
                overlay.setGravity(Gravity.CENTER_VERTICAL);
                overlay.setPadding(dp(act, 12), dp(act, 10), dp(act, 12), dp(act, 10));
                overlay.setClickable(true);

                // Стиль карточки: идентичен меню плагина (theme.surface + скругления + theme.outline)
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(theme.surface);
                bg.setCornerRadius(dp(act, 18));
                bg.setStroke(dp(act, 1), theme.outline);
                overlay.setBackground(bg);

                if (Build.VERSION.SDK_INT >= 21) {
                    overlay.setElevation(dp(act, 12));
                }

                // Векторная иконка скачивания или звука в контейнере theme.primaryContainer
                FrameLayout iconBox = new FrameLayout(act);
                int boxSize = dp(act, 34);
                LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(boxSize, boxSize);
                boxLp.rightMargin = dp(act, 10);
                iconBox.setLayoutParams(boxLp);

                GradientDrawable iconBg = new GradientDrawable();
                iconBg.setColor(theme.primaryContainer);
                iconBg.setCornerRadius(dp(act, 10));
                iconBg.setStroke(dp(act, 1), theme.outline);
                iconBox.setBackground(iconBg);

                ImageView dlIcon = new ImageView(act);
                Drawable dlDraw = isAudio ? new MusicIconDrawable(theme.primary) : new DownloadIconDrawable(theme.primary);
                int dlSize = dp(act, 18);
                dlDraw.setBounds(0, 0, dlSize, dlSize);
                dlIcon.setImageDrawable(dlDraw);
                FrameLayout.LayoutParams dlLp = new FrameLayout.LayoutParams(dlSize, dlSize, Gravity.CENTER);
                dlIcon.setLayoutParams(dlLp);
                iconBox.addView(dlIcon);
                overlay.addView(iconBox);

                // Центральная колонка: строка с названием и процентами + плавный индикатор прогресса
                LinearLayout centerCol = new LinearLayout(act);
                centerCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                cLp.rightMargin = dp(act, 10);
                centerCol.setLayoutParams(cLp);

                LinearLayout titleRow = new LinearLayout(act);
                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                titleRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView titleView = new TextView(act);
                titleView.setText((isAudio ? "Скачивание звука • " : "Скачивание • ") + label);
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                titleView.setTextColor(theme.onSurface);
                titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                titleView.setMaxLines(1);
                titleView.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                titleView.setLayoutParams(tLp);
                titleRow.addView(titleView);

                TextView percentView = new TextView(act);
                percentView.setText("0%");
                percentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                percentView.setTextColor(theme.primary);
                percentView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                percentView.setPadding(dp(act, 6), 0, 0, 0);
                titleRow.addView(percentView);
                activeProgressPercentView = percentView;

                centerCol.addView(titleRow);

                // Горизонтальный скруглённый индикатор прогресса в цветах темы
                ProgressBar pBar = new ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal);
                pBar.setMax(100);
                pBar.setProgress(0);

                GradientDrawable track = new GradientDrawable();
                track.setColor(theme.surfaceContainer);
                track.setCornerRadius(dp(act, 3));

                GradientDrawable progressShape = new GradientDrawable();
                progressShape.setColor(theme.primary);
                progressShape.setCornerRadius(dp(act, 3));
                ClipDrawable clipProgress = new ClipDrawable(progressShape, Gravity.LEFT, ClipDrawable.HORIZONTAL);

                Drawable[] layers = new Drawable[]{ track, clipProgress };
                LayerDrawable layerDrawable = new LayerDrawable(layers);
                layerDrawable.setId(0, android.R.id.background);
                layerDrawable.setId(1, android.R.id.progress);
                pBar.setProgressDrawable(layerDrawable);

                LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 5));
                pbLp.topMargin = dp(act, 5);
                pBar.setLayoutParams(pbLp);
                centerCol.addView(pBar);
                activeProgressBar = pBar;

                overlay.addView(centerCol);

                // Кнопка отмены скачивания (✕) в стиле круглых кнопок плагина
                ImageView cancelBtn = new ImageView(act);
                CrossIconDrawable crossDraw = new CrossIconDrawable(theme.onSurfaceVariant);
                int crossIconSize = dp(act, 14);
                crossDraw.setBounds(0, 0, crossIconSize, crossIconSize);
                cancelBtn.setImageDrawable(crossDraw);

                int btnSize = dp(act, 32);
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(btnSize, btnSize);
                cancelBtn.setLayoutParams(btnLp);
                cancelBtn.setPadding(dp(act, 7), dp(act, 7), dp(act, 7), dp(act, 7));

                GradientDrawable btnBg = new GradientDrawable();
                btnBg.setShape(GradientDrawable.OVAL);
                btnBg.setColor(theme.surfaceContainer);
                btnBg.setStroke(dp(act, 1), theme.divider);
                cancelBtn.setBackground(btnBg);

                cancelBtn.setOnClickListener(v -> {
                    hapticClick(cancelBtn);
                    isDownloadCancelled = true;
                    hideDownloadOverlay();
                    safeToast(act, "Загрузка отменена");
                });

                overlay.addView(cancelBtn);

                int displayW = act.getResources().getDisplayMetrics().widthPixels;
                int maxW = Math.min(dp(act, 350), (int) (displayW * 0.92f));
                FrameLayout.LayoutParams fLp = new FrameLayout.LayoutParams(maxW, ViewGroup.LayoutParams.WRAP_CONTENT);
                fLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                // Смещаем вниз, чтобы плашка не перекрывала верхние вкладки TikTok (Рекомендации, Подписки, Поиск)
                fLp.topMargin = dp(act, 108);

                decor.addView(overlay, fLp);
                activeProgressOverlay = overlay;

            } catch (Throwable t) {
                margyt().log("vqp showDownloadOverlay: " + t);
            }
        });
    }

    private void updateDownloadOverlay(final int percent) {
        MAIN.post(() -> {
            try {
                if (activeProgressPercentView != null) {
                    activeProgressPercentView.setText(percent + "%");
                }
                if (activeProgressBar != null) {
                    activeProgressBar.setProgress(percent);
                }
            } catch (Throwable ignored) {}
        });
    }

    private void hideDownloadOverlay() {
        MAIN.post(() -> {
            try {
                Activity act = activeDownloadActivity;
                if (act == null) act = margyt().screen();
                hideDownloadOverlayInternal(act);
            } catch (Throwable ignored) {}
        });
    }

    private void hideDownloadOverlayInternal(Activity act) {
        try {
            if (activeProgressOverlay != null) {
                if (activeProgressOverlay.getParent() instanceof ViewGroup) {
                    ((ViewGroup) activeProgressOverlay.getParent()).removeView(activeProgressOverlay);
                }
                activeProgressOverlay = null;
            }
            activeProgressPercentView = null;
            activeProgressBar = null;
        } catch (Throwable ignored) {}
    }

    private void startStreamingDownload(final Activity activity, final Quality q, final String author) {
        String cleanProgressLabel = q.resolution.split("\\(")[0].trim();
        isDownloadCancelled = false;
        showDownloadOverlay(activity, cleanProgressLabel, q.isAudio);

        margyt().away("download", () -> {
            File tempFile = null;
            try {
                Context ctx = margyt().context();
                String cleanAuthor = sanitizeFileName(author);
                String authorPrefix = cleanAuthor.isEmpty() ? "tiktok_" : cleanAuthor + "_";
                String ext = q.isAudio ? ".mp3" : ".mp4";
                String cleanRes;
                if (q.isAudio) {
                    cleanRes = "audio";
                } else if (q.resolution.toLowerCase().contains("слайд") || q.resolution.toLowerCase().contains("slide")) {
                    cleanRes = "slideshow";
                } else {
                    cleanRes = sanitizeFileName(q.resolution.split("\\(")[0].trim());
                }
                String fileName = authorPrefix + System.currentTimeMillis() + "_" + cleanRes + ext;

                boolean success = false;
                File cacheDir = ctx.getCacheDir();
                tempFile = new File(cacheDir, "temp_" + System.currentTimeMillis() + ext);

                for (String url : q.urls) {
                    if (isDownloadCancelled) break;
                    margyt().log("vqp: загрузка с " + url);
                    if (downloadToFile(url, tempFile, cleanProgressLabel)) {
                        success = true;
                        break;
                    }
                }

                if (isDownloadCancelled) {
                    if (tempFile != null && tempFile.exists()) tempFile.delete();
                    hideDownloadOverlay();
                    return;
                }

                if (!success || !tempFile.exists() || tempFile.length() == 0) {
                    hideDownloadOverlay();
                    if (tempFile != null) tempFile.delete();
                    safeAlert(activity, "Ошибка скачивания",
                            "Не удалось загрузить " + (q.isAudio ? "звук" : "видео") + ". Попробуйте выбрать другой вариант.");
                    return;
                }

                saveTempToDownloads(ctx, tempFile, fileName);
                tempFile.delete();

                hideDownloadOverlay();
                hapticSuccess(ctx);
                String targetFolder = getSavedFolder();
                safeToast(activity, "Сохранено в " + targetFolder + ":\n" + fileName);
                margyt().log("vqp: сохранено " + fileName);

            } catch (Throwable t) {
                hideDownloadOverlay();
                if (tempFile != null) tempFile.delete();
                if (isDownloadCancelled) return;
                margyt().log("vqp download error: " + t);
                safeAlert(activity, "Ошибка сохранения", "Не удалось сохранить файл: " + t.getMessage());
            }
        });
    }

    private boolean downloadToFile(String urlStr, File dest, String resLabel) {
        HttpURLConnection conn = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "keep-alive");

            int code = conn.getResponseCode();
            if (code != 200 && code != 206) {
                return false;
            }

            long total = conn.getContentLength();
            in = conn.getInputStream();
            out = new FileOutputStream(dest);

            byte[] buffer = new byte[32768];
            long got = 0;
            int read;
            int lastPercent = -1;

            while ((read = in.read(buffer)) != -1) {
                if (isDownloadCancelled) {
                    if (dest != null && dest.exists()) dest.delete();
                    return false;
                }
                out.write(buffer, 0, read);
                got += read;
                if (total > 0) {
                    int percent = (int) (got * 100 / total);
                    if (percent != lastPercent && percent % 2 == 0) {
                        lastPercent = percent;
                        updateDownloadOverlay(percent);
                    }
                }
            }
            out.flush();
            if (isDownloadCancelled) {
                if (dest != null && dest.exists()) dest.delete();
                return false;
            }
            return got > 0;
        } catch (Throwable t) {
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    // ─────────────────────────────────────────── settings dialog ─────────────

    private void openSettingsDialog(final Activity activity) {
        MAIN.post(() -> {
            try {
                Activity act = getLiveActivity(activity);
                if (act == null || act.isFinishing()) return;

                final Dialog dialog = new Dialog(act);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Window win = dialog.getWindow();
                if (win != null) {
                    win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    win.setDimAmount(0.55f);
                }

                final SystemTheme theme = new SystemTheme(act);

                LinearLayout card = new LinearLayout(act);
                card.setOrientation(LinearLayout.VERTICAL);
                int pad = dp(act, 20);
                card.setPadding(pad, pad, pad, dp(act, 16));

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(theme.surface);
                cardBg.setCornerRadius(dp(act, 22));
                cardBg.setStroke(dp(act, 1), theme.outline);
                card.setBackground(cardBg);

                // Шапка настроек с векторной шестерёнкой
                LinearLayout header = new LinearLayout(act);
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(Gravity.CENTER_VERTICAL);
                header.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                ImageView gearIcon = new ImageView(act);
                GearIconDrawable gearD = new GearIconDrawable(theme.primary);
                int gSize = dp(act, 20);
                gearD.setBounds(0, 0, gSize, gSize);
                gearIcon.setImageDrawable(gearD);
                LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(gSize, gSize);
                gLp.rightMargin = dp(act, 8);
                gearIcon.setLayoutParams(gLp);
                header.addView(gearIcon);

                TextView title = new TextView(act);
                title.setText("Настройки плагина");
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.5f);
                title.setTextColor(theme.onSurface);
                title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                header.addView(title);
                card.addView(header);

                TextView sub = new TextView(act);
                sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                sub.setPadding(0, dp(act, 3), 0, dp(act, 14));

                String prefix = "Video Quality Picker • v2.11 • ";
                String nick = "fodis3";
                String fullText = prefix + nick;

                SpannableStringBuilder ssb = new SpannableStringBuilder(fullText);
                ssb.setSpan(new ForegroundColorSpan(theme.onSurfaceVariant), 0, prefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(theme.tertiary), prefix.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), prefix.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new UnderlineSpan(), prefix.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                sub.setText(ssb);

                sub.setOnClickListener(v -> {
                    hapticClick(v);
                    try {
                        dialog.dismiss();
                    } catch (Throwable ignored) {}
                    openTikTokProfile(act, "fodis3");
                });

                card.addView(sub);

                // ── Секция 1: Прозрачность ──
                TextView alphaTitle = new TextView(act);
                alphaTitle.setText("Прозрачность кнопки");
                alphaTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
                alphaTitle.setTextColor(theme.onSurface);
                alphaTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                card.addView(alphaTitle);

                LinearLayout alphaRow = new LinearLayout(act);
                alphaRow.setOrientation(LinearLayout.HORIZONTAL);
                alphaRow.setPadding(0, dp(act, 5), 0, dp(act, 12));

                final float[] alphaVals = {0.25f, 0.50f, 0.75f, 1.0f};
                final String[] alphaLabels = {"25%", "50%", "75%", "100%"};
                final List<TextView> alphaChips = new ArrayList<>();

                float currentAlpha = getSavedAlpha();

                for (int i = 0; i < alphaVals.length; i++) {
                    final float val = alphaVals[i];
                    final TextView chip = new TextView(act);
                    chip.setText(alphaLabels[i]);
                    chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                    chip.setGravity(Gravity.CENTER);
                    chip.setPadding(dp(act, 8), dp(act, 7), dp(act, 8), dp(act, 7));

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    if (i > 0) lp.leftMargin = dp(act, 5);
                    chip.setLayoutParams(lp);

                    alphaChips.add(chip);
                    styleSettingChip(chip, act, Math.abs(currentAlpha - val) < 0.05f, theme);

                    chip.setOnClickListener(v -> {
                        hapticTick(v);
                        try {
                            margyt().prefs().edit().putFloat("btn_alpha", val).apply();
                        } catch (Throwable ignored) {}
                        for (int j = 0; j < alphaVals.length; j++) {
                            styleSettingChip(alphaChips.get(j), act, Math.abs(alphaVals[j] - val) < 0.05f, theme);
                        }
                        updateActiveFloatingButton(act);
                    });

                    alphaRow.addView(chip);
                }
                card.addView(alphaRow);

                // ── Секция 2: Размер кнопки ──
                TextView sizeTitle = new TextView(act);
                sizeTitle.setText("Размер кнопки");
                sizeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
                sizeTitle.setTextColor(theme.onSurface);
                sizeTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                card.addView(sizeTitle);

                LinearLayout sizeRow = new LinearLayout(act);
                sizeRow.setOrientation(LinearLayout.HORIZONTAL);
                sizeRow.setPadding(0, dp(act, 5), 0, dp(act, 12));

                final String[] sizeVals = {"small", "normal", "large"};
                final String[] sizeLabels = {"Мелкий", "Обычный", "Крупный"};
                final List<TextView> sizeChips = new ArrayList<>();

                String currentSize = getSavedSize();

                for (int i = 0; i < sizeVals.length; i++) {
                    final String sVal = sizeVals[i];
                    final TextView chip = new TextView(act);
                    chip.setText(sizeLabels[i]);
                    chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                    chip.setGravity(Gravity.CENTER);
                    chip.setPadding(dp(act, 8), dp(act, 7), dp(act, 8), dp(act, 7));

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    if (i > 0) lp.leftMargin = dp(act, 5);
                    chip.setLayoutParams(lp);

                    sizeChips.add(chip);
                    styleSettingChip(chip, act, sVal.equals(currentSize), theme);

                    chip.setOnClickListener(v -> {
                        hapticTick(v);
                        try {
                            margyt().prefs().edit().putString("btn_size", sVal).apply();
                        } catch (Throwable ignored) {}
                        for (int j = 0; j < sizeVals.length; j++) {
                            styleSettingChip(sizeChips.get(j), act, sizeVals[j].equals(sVal), theme);
                        }
                        updateActiveFloatingButton(act);
                    });

                    sizeRow.addView(chip);
                }
                card.addView(sizeRow);

                // ── Секция 3: Путь сохранения видео ──
                TextView folderTitle = new TextView(act);
                folderTitle.setText("Папка сохранения видео");
                folderTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
                folderTitle.setTextColor(theme.onSurface);
                folderTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                card.addView(folderTitle);

                final TextView curPathView = new TextView(act);
                final String savedFolder = getSavedFolder();
                curPathView.setText("Текущий путь: /" + savedFolder + "/");
                curPathView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                curPathView.setTextColor(theme.primary);
                curPathView.setPadding(0, dp(act, 2), 0, dp(act, 6));
                card.addView(curPathView);

                LinearLayout folderRow = new LinearLayout(act);
                folderRow.setOrientation(LinearLayout.HORIZONTAL);
                folderRow.setPadding(0, 0, 0, dp(act, 8));

                final String[] folderKeys = {"Download", "Download/TikTok", "Download/MargyT", "Movies/TikTok"};
                final String[] folderLabels = {"Загрузки", "TikTok", "MargyT", "Фильмы"};
                final List<TextView> folderChips = new ArrayList<>();

                for (int i = 0; i < folderKeys.length; i++) {
                    final String fKey = folderKeys[i];
                    final TextView chip = new TextView(act);
                    chip.setText(folderLabels[i]);
                    chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                    chip.setGravity(Gravity.CENTER);
                    chip.setPadding(dp(act, 6), dp(act, 6), dp(act, 6), dp(act, 6));

                    LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    if (i > 0) clp.leftMargin = dp(act, 4);
                    chip.setLayoutParams(clp);

                    styleSettingChip(chip, act, fKey.equalsIgnoreCase(savedFolder), theme);
                    folderChips.add(chip);

                    chip.setOnClickListener(v -> {
                        hapticTick(v);
                        try {
                            margyt().prefs().edit()
                                    .remove("save_folder_tree_uri")
                                    .putString("save_folder", fKey)
                                    .apply();
                        } catch (Throwable ignored) {}
                        for (int j = 0; j < folderKeys.length; j++) {
                            styleSettingChip(folderChips.get(j), act, folderKeys[j].equalsIgnoreCase(fKey), theme);
                        }
                        curPathView.setText("Текущий путь: /" + fKey + "/");
                    });
                    folderRow.addView(chip);
                }
                card.addView(folderRow);

                // Кнопка открытия системного файлового менеджера (проводника)
                TextView pickFolderBtn = new TextView(act);
                pickFolderBtn.setText("Выбрать папку в проводнике");
                pickFolderBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                pickFolderBtn.setTextColor(theme.primary);
                pickFolderBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                pickFolderBtn.setGravity(Gravity.CENTER);
                pickFolderBtn.setPadding(dp(act, 12), dp(act, 9), dp(act, 12), dp(act, 9));

                FolderIconDrawable folderDraw = new FolderIconDrawable(theme.primary);
                int folderIconSize = dp(act, 16);
                folderDraw.setBounds(0, 0, folderIconSize, folderIconSize);
                pickFolderBtn.setCompoundDrawables(folderDraw, null, null, null);
                pickFolderBtn.setCompoundDrawablePadding(dp(act, 8));

                GradientDrawable pickBg = new GradientDrawable();
                pickBg.setColor(theme.surfaceContainer);
                pickBg.setCornerRadius(dp(act, 14));
                pickBg.setStroke(dp(act, 1), theme.primary);
                pickFolderBtn.setBackground(pickBg);

                pickFolderBtn.setOnClickListener(v -> {
                    hapticClick(v);
                    try {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        FolderPickerFragment frag = new FolderPickerFragment();
                        frag.setup(VideoQualityPicker.this, curPathView);
                        act.getFragmentManager().beginTransaction().add(frag, "vqp_tree_picker").commitAllowingStateLoss();
                        act.getFragmentManager().executePendingTransactions();
                        frag.startActivityForResult(intent, FolderPickerFragment.REQ_CODE);
                    } catch (Throwable t) {
                        margyt().log("vqp open document tree error: " + t);
                        safeAlert(act, "Ошибка проводника", "Не удалось запустить системный файловый менеджер: " + t.getMessage());
                    }
                });
                card.addView(pickFolderBtn);

                // Кнопка для ручного указания любой папки
                TextView customFolderBtn = new TextView(act);
                customFolderBtn.setText("или ввести путь вручную...");
                customFolderBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                customFolderBtn.setTextColor(theme.onSurfaceVariant);
                customFolderBtn.setGravity(Gravity.CENTER);
                customFolderBtn.setPadding(0, dp(act, 6), 0, dp(act, 12));
                customFolderBtn.setOnClickListener(v -> {
                    hapticTick(v);
                    AlertDialog.Builder ab = new AlertDialog.Builder(act);
                    ab.setTitle("Путь сохранения видео");
                    ab.setMessage("Укажите подпапку (например: Download/TikTok или Movies/TikTok):");
                    final EditText input = new EditText(act);
                    input.setText(getSavedFolder());
                    ab.setView(input);
                    ab.setPositiveButton("Сохранить", (d, which) -> {
                        String userVal = input.getText().toString().trim();
                        if (userVal.isEmpty()) userVal = "Download";
                        userVal = userVal.replace("\\", "/").replaceAll("^/+|/+$", "");
                        try {
                            margyt().prefs().edit()
                                    .remove("save_folder_tree_uri")
                                    .putString("save_folder", userVal)
                                    .apply();
                        } catch (Throwable ignored) {}
                        curPathView.setText("Текущий путь: /" + userVal + "/");
                        for (int j = 0; j < folderKeys.length; j++) {
                            styleSettingChip(folderChips.get(j), act, folderKeys[j].equalsIgnoreCase(userVal), theme);
                        }
                    });
                    ab.setNegativeButton("Отмена", null);
                    ab.show();
                });
                card.addView(customFolderBtn);

                // ── Секция 4: Позиция кнопки ──
                TextView resetBtn = new TextView(act);
                resetBtn.setText("Сбросить положение кнопки в угол");
                resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                resetBtn.setTextColor(theme.onSurfaceVariant);
                resetBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                resetBtn.setGravity(Gravity.CENTER);
                resetBtn.setPadding(dp(act, 12), dp(act, 9), dp(act, 12), dp(act, 9));

                GradientDrawable resetBg = new GradientDrawable();
                resetBg.setColor(theme.surfaceContainer);
                resetBg.setCornerRadius(dp(act, 14));
                resetBg.setStroke(dp(act, 1), theme.divider);
                resetBtn.setBackground(resetBg);

                resetBtn.setOnClickListener(v -> {
                    hapticClick(v);
                    try {
                        margyt().prefs().edit().remove("btn_x").remove("btn_y").apply();
                    } catch (Throwable ignored) {}
                    resetButtonPosition(act);
                    safeToast(act, "Позиция сброшена");
                });
                card.addView(resetBtn);

                TextView hint = new TextView(act);
                hint.setText("Кнопку можно свободно перетаскивать пальцем по экрану.");
                hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                hint.setTextColor(theme.onSurfaceVariant);
                hint.setPadding(0, dp(act, 4), 0, dp(act, 12));
                card.addView(hint);

                // ── Секция 5: Тактильная отдача (вибрация) ──
                TextView hapticTitle = new TextView(act);
                hapticTitle.setText("Тактильная отдача (вибрация)");
                hapticTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
                hapticTitle.setTextColor(theme.onSurface);
                hapticTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                card.addView(hapticTitle);

                LinearLayout hapticRow = new LinearLayout(act);
                hapticRow.setOrientation(LinearLayout.HORIZONTAL);
                hapticRow.setPadding(0, dp(act, 5), 0, dp(act, 12));

                final boolean currentHaptic = isHapticsEnabled();
                final TextView hapticOn = new TextView(act);
                hapticOn.setText("Включена");
                hapticOn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                hapticOn.setGravity(Gravity.CENTER);
                hapticOn.setPadding(dp(act, 8), dp(act, 7), dp(act, 8), dp(act, 7));

                final TextView hapticOff = new TextView(act);
                hapticOff.setText("Выключена");
                hapticOff.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                hapticOff.setGravity(Gravity.CENTER);
                hapticOff.setPadding(dp(act, 8), dp(act, 7), dp(act, 8), dp(act, 7));

                LinearLayout.LayoutParams hLp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                hapticOn.setLayoutParams(hLp1);
                LinearLayout.LayoutParams hLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                hLp2.leftMargin = dp(act, 6);
                hapticOff.setLayoutParams(hLp2);

                styleSettingChip(hapticOn, act, currentHaptic, theme);
                styleSettingChip(hapticOff, act, !currentHaptic, theme);

                hapticOn.setOnClickListener(v -> {
                    try {
                        margyt().prefs().edit().putBoolean("haptics_enabled", true).apply();
                    } catch (Throwable ignored) {}
                    styleSettingChip(hapticOn, act, true, theme);
                    styleSettingChip(hapticOff, act, false, theme);
                    hapticClick(v);
                });

                hapticOff.setOnClickListener(v -> {
                    try {
                        margyt().prefs().edit().putBoolean("haptics_enabled", false).apply();
                    } catch (Throwable ignored) {}
                    styleSettingChip(hapticOn, act, false, theme);
                    styleSettingChip(hapticOff, act, true, theme);
                });

                hapticRow.addView(hapticOn);
                hapticRow.addView(hapticOff);
                card.addView(hapticRow);

                // Кнопка закрытия
                TextView doneBtn = new TextView(act);
                doneBtn.setText("Готово");
                doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                doneBtn.setTextColor(theme.onPrimary);
                doneBtn.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                doneBtn.setGravity(Gravity.CENTER);
                doneBtn.setPadding(0, dp(act, 10), 0, dp(act, 10));

                GradientDrawable doneBg = new GradientDrawable();
                doneBg.setColor(theme.primary);
                doneBg.setCornerRadius(dp(act, 16));
                doneBtn.setBackground(doneBg);

                doneBtn.setOnClickListener(v -> {
                    hapticTick(v);
                    dialog.dismiss();
                });
                card.addView(doneBtn);

                dialog.setContentView(card);

                int displayW = act.getResources().getDisplayMetrics().widthPixels;
                int cardW = Math.min(dp(act, 350), (int) (displayW * 0.90f));
                dialog.getWindow().setLayout(cardW, ViewGroup.LayoutParams.WRAP_CONTENT);

                dialog.show();
            } catch (Throwable t) {
                margyt().log("vqp openSettingsDialog error: " + t);
            }
        });
    }

    /**
     * Открытие профиля пользователя внутри приложения TikTok (без перехода в сторонний браузер).
     */
    private void openTikTokProfile(final Activity act, final String username) {
        if (act == null || act.isFinishing()) return;
        MAIN.post(() -> {
            try {
                final String profileUrl = "https://www.tiktok.com/@" + username;
                final String awemeSchema = "aweme://user/profile?unique_id=" + username;
                final String snss1233Schema = "snssdk1233://user/profile?unique_id=" + username;
                final String snss1180Schema = "snssdk1180://user/profile?unique_id=" + username;

                // 1. Попытка открыть через внутренний SmartRouter ByteDance / TikTok
                String[] routerClasses = {
                    "com.bytedance.router.SmartRouter",
                    "com.ss.android.ugc.aweme.router.SmartRouter"
                };
                String[] routerUrls = {
                    awemeSchema,
                    profileUrl,
                    snss1233Schema,
                    snss1180Schema
                };

                for (String clsName : routerClasses) {
                    try {
                        Class<?> srCls = Class.forName(clsName);
                        Method buildRoute = srCls.getMethod("buildRoute", Context.class, String.class);
                        for (String rUrl : routerUrls) {
                            try {
                                Object routeObj = buildRoute.invoke(null, act, rUrl);
                                if (routeObj != null) {
                                    Method openM = null;
                                    try {
                                        openM = routeObj.getClass().getMethod("open");
                                    } catch (NoSuchMethodException e) {
                                        for (Method m : routeObj.getClass().getMethods()) {
                                            if ("open".equals(m.getName()) && m.getParameterTypes().length == 0) {
                                                openM = m;
                                                break;
                                            }
                                        }
                                    }
                                    if (openM != null) {
                                        openM.invoke(routeObj);
                                        return;
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    } catch (Throwable ignored) {}
                }

                // 2. Попытка открыть через Intent, строго привязанный к текущему пакету TikTok (act.getPackageName())
                String pkg = act.getPackageName();
                String[] intentUris = {
                    awemeSchema,
                    snss1233Schema,
                    snss1180Schema,
                    "tiktok://user/profile?unique_id=" + username,
                    profileUrl
                };

                for (String uriStr : intentUris) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
                        intent.setPackage(pkg);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (intent.resolveActivity(act.getPackageManager()) != null) {
                            act.startActivity(intent);
                            return;
                        }
                    } catch (Throwable ignored) {}
                }

                // 3. Прямой запуск Intent внутри пакета без предварительной фильтрации
                try {
                    Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(profileUrl));
                    fallback.setPackage(pkg);
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(fallback);
                    return;
                } catch (Throwable ignored) {}

                // 4. Резервный системный Intent на случай нестандартных окружений
                try {
                    Intent sysFallback = new Intent(Intent.ACTION_VIEW, Uri.parse(profileUrl));
                    sysFallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(sysFallback);
                } catch (Throwable t) {
                    Toast.makeText(act, "Не удалось открыть профиль @" + username, Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable t) {
                margyt().log("vqp openTikTokProfile error: " + t);
            }
        });
    }

    private static void styleSettingChip(TextView chip, Context ctx, boolean isSelected, SystemTheme theme) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 12));
        if (isSelected) {
            bg.setColor(theme.primary);
            chip.setTextColor(theme.onPrimary);
            chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        } else {
            bg.setColor(theme.surfaceContainer);
            bg.setStroke(dp(ctx, 1), theme.divider);
            chip.setTextColor(theme.onSurfaceVariant);
            chip.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        chip.setBackground(bg);
    }

    public void handlePickedTreeUri(final Activity act, final Uri treeUri, final TextView curPathView) {
        if (treeUri == null || act == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 19) {
                try {
                    act.getContentResolver().takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );
                } catch (Throwable t) {
                    margyt().log("vqp takePersistableUriPermission: " + t);
                }
            }

            String docId = null;
            if (Build.VERSION.SDK_INT >= 21) {
                try {
                    docId = DocumentsContract.getTreeDocumentId(treeUri);
                } catch (Throwable ignored) {}
            }
            if (docId == null) docId = treeUri.getLastPathSegment();

            String displayPath = docId;
            if (displayPath != null) {
                if (displayPath.startsWith("primary:")) {
                    displayPath = displayPath.substring("primary:".length());
                }
            } else {
                displayPath = "Выбранная папка";
            }

            final String finalPath = displayPath;
            margyt().prefs().edit()
                    .putString("save_folder_tree_uri", treeUri.toString())
                    .putString("save_folder", finalPath)
                    .apply();

            MAIN.post(() -> {
                if (curPathView != null) {
                    curPathView.setText("Текущий путь: /" + finalPath + "/");
                }
                safeToast(act, "Выбрана папка: " + finalPath);
            });
        } catch (Throwable t) {
            margyt().log("vqp handlePickedTreeUri: " + t);
        }
    }

    private void updateActiveFloatingButton(Activity act) {
        MAIN.post(() -> {
            try {
                if (act == null || act.isFinishing()) return;
                ViewGroup decor = (ViewGroup) act.getWindow().getDecorView();
                View v = decor.findViewWithTag(BUTTON_TAG);
                if (v instanceof TextView) {
                    TextView btn = (TextView) v;
                    btn.setAlpha(getSavedAlpha());
                    applyButtonSize(btn, act, getSavedSize());
                }
            } catch (Throwable ignored) {}
        });
    }

    private void resetButtonPosition(Activity act) {
        MAIN.post(() -> {
            try {
                if (act == null || act.isFinishing()) return;
                ViewGroup decor = (ViewGroup) act.getWindow().getDecorView();
                View v = decor.findViewWithTag(BUTTON_TAG);
                if (v != null) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
                    lp.gravity = Gravity.TOP | Gravity.END;
                    lp.topMargin = dp(act, 90);
                    lp.rightMargin = dp(act, 12);
                    lp.leftMargin = 0;
                    lp.bottomMargin = 0;
                    v.setLayoutParams(lp);
                }
            } catch (Throwable ignored) {}
        });
    }

    private float getSavedAlpha() {
        try {
            return margyt().prefs().getFloat("btn_alpha", 0.50f);
        } catch (Throwable ignored) {
            return 0.50f;
        }
    }

    private String getSavedSize() {
        try {
            return margyt().prefs().getString("btn_size", "normal");
        } catch (Throwable ignored) {
            return "normal";
        }
    }

    private String getSavedFolder() {
        try {
            String folder = margyt().prefs().getString("save_folder", "Download");
            if (folder == null || folder.trim().isEmpty()) return "Download";
            return folder.trim();
        } catch (Throwable ignored) {
            return "Download";
        }
    }

    private static void applyButtonSize(TextView btn, Context ctx, String size) {
        int padH, padV, radius, icSize;
        float textSize;
        if ("small".equals(size)) {
            padH = dp(ctx, 8);
            padV = dp(ctx, 4);
            textSize = 10.5f;
            radius = dp(ctx, 12);
            icSize = dp(ctx, 12);
        } else if ("large".equals(size)) {
            padH = dp(ctx, 14);
            padV = dp(ctx, 8);
            textSize = 13.5f;
            radius = dp(ctx, 18);
            icSize = dp(ctx, 16);
        } else { // normal
            padH = dp(ctx, 11);
            padV = dp(ctx, 6);
            textSize = 12f;
            radius = dp(ctx, 15);
            icSize = dp(ctx, 14);
        }
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        btn.setPadding(padH, padV, padH, padV);

        DownloadIconDrawable icon = new DownloadIconDrawable(0xFFFFFFFF);
        icon.setBounds(0, 0, icSize, icSize);
        btn.setCompoundDrawables(icon, null, null, null);
        btn.setCompoundDrawablePadding(dp(ctx, 4));
        btn.setText("HD");

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(0x88101014);
        bg.setCornerRadius(radius);
        bg.setStroke(dp(ctx, 1), 0x55FFFFFF);
        btn.setBackground(bg);
    }

    private void saveTempToDownloads(Context ctx, File tempFile, String fileName) throws Exception {
        final long nowMs = System.currentTimeMillis();
        final long nowSec = nowMs / 1000;
        try {
            tempFile.setLastModified(nowMs);
        } catch (Throwable ignored) {}

        // 1. Проверяем, есть ли сохранённая папка из системного проводника (SAF Tree Uri)
        String treeUriStr = null;
        try {
            treeUriStr = margyt().prefs().getString("save_folder_tree_uri", null);
        } catch (Throwable ignored) {}

        if (treeUriStr != null && !treeUriStr.isEmpty() && Build.VERSION.SDK_INT >= 21) {
            try {
                Uri treeUri = Uri.parse(treeUriStr);
                String docId = DocumentsContract.getTreeDocumentId(treeUri);
                Uri docTreeRoot = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                boolean isAudio = fileName.endsWith(".mp3") || fileName.endsWith(".m4a");
                String mimeType = isAudio ? "audio/mpeg" : "video/mp4";
                Uri docUri = DocumentsContract.createDocument(ctx.getContentResolver(), docTreeRoot, mimeType, fileName);
                if (docUri != null) {
                    try (InputStream in = new java.io.FileInputStream(tempFile);
                         OutputStream out = ctx.getContentResolver().openOutputStream(docUri)) {
                        byte[] buf = new byte[32768];
                        int r;
                        while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                    }
                    try {
                        ContentValues dcv = new ContentValues();
                        dcv.put(DocumentsContract.Document.COLUMN_LAST_MODIFIED, nowMs);
                        ctx.getContentResolver().update(docUri, dcv, null, null);
                    } catch (Throwable ignored) {}
                    return; // Успешно сохранено в пользовательскую папку
                }
            } catch (Throwable t) {
                margyt().log("vqp save to tree uri error, falling back to MediaStore: " + t);
            }
        }

        // 2. Стандартное сохранение через MediaStore / External Storage
        String subFolder = getSavedFolder().replace('\\', '/').replaceAll("^/+|/+$", "");
        if (subFolder.isEmpty()) subFolder = "Download";

        boolean isAudio = fileName.endsWith(".mp3") || fileName.endsWith(".m4a");
        String mimeType = isAudio ? "audio/mpeg" : "video/mp4";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            cv.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            cv.put(MediaStore.Downloads.IS_PENDING, 1);
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, subFolder + "/");
            cv.put(MediaStore.MediaColumns.DATE_ADDED, nowSec);
            cv.put(MediaStore.MediaColumns.DATE_MODIFIED, nowSec);
            if (!isAudio && Build.VERSION.SDK_INT >= 29) {
                cv.put(MediaStore.Video.Media.DATE_TAKEN, nowMs);
            }

            Uri col = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri item = null;
            try {
                item = ctx.getContentResolver().insert(col, cv);
            } catch (Throwable t) {
                // В случае ошибки с нестандартным путём возвращаемся к надежному Download/
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/");
                item = ctx.getContentResolver().insert(col, cv);
            }
            if (item == null) throw new RuntimeException("MediaStore insert failed");

            try (InputStream in = new java.io.FileInputStream(tempFile);
                 OutputStream out = ctx.getContentResolver().openOutputStream(item)) {
                byte[] buf = new byte[32768];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            }
            cv.clear();
            cv.put(MediaStore.Downloads.IS_PENDING, 0);
            cv.put(MediaStore.MediaColumns.DATE_MODIFIED, nowSec);
            if (!isAudio && Build.VERSION.SDK_INT >= 29) {
                cv.put(MediaStore.Video.Media.DATE_TAKEN, nowMs);
            }
            ctx.getContentResolver().update(item, cv, null, null);

            // Фиксация даты скачивания в MediaStore:
            if (!isAudio) {
                final Uri finalItem = item;
                MAIN.postDelayed(() -> {
                    try {
                        ContentValues vcv = new ContentValues();
                        vcv.put(MediaStore.Video.Media.DATE_TAKEN, nowMs);
                        vcv.put(MediaStore.Video.Media.DATE_MODIFIED, nowSec);
                        vcv.put(MediaStore.Video.Media.DATE_ADDED, nowSec);
                        ctx.getContentResolver().update(finalItem, vcv, null, null);
                        ctx.getContentResolver().update(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                vcv,
                                MediaStore.Video.Media.DISPLAY_NAME + "=?",
                                new String[]{fileName}
                        );
                    } catch (Throwable ignored) {}
                }, 1200);
            }
        } else {
            File base = Environment.getExternalStorageDirectory();
            File dir = new File(base, subFolder);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, fileName);
            try (InputStream in = new java.io.FileInputStream(tempFile);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[32768];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            }
            try {
                dest.setLastModified(nowMs);
            } catch (Throwable ignored) {}

            try {
                android.media.MediaScannerConnection.scanFile(ctx, new String[]{dest.getAbsolutePath()}, new String[]{mimeType}, (path, uri) -> {
                    if (uri != null) {
                        try {
                            ContentValues mcv = new ContentValues();
                            if (!isAudio) mcv.put(MediaStore.Video.Media.DATE_TAKEN, nowMs);
                            mcv.put(MediaStore.MediaColumns.DATE_MODIFIED, nowSec);
                            mcv.put(MediaStore.MediaColumns.DATE_ADDED, nowSec);
                            ctx.getContentResolver().update(uri, mcv, null, null);
                        } catch (Throwable ignored) {}
                    }
                });
            } catch (Throwable ignored) {}
        }
    }

    // ─────────────────────────────────────────── post analysis & cache ────────

    private void processPost(Object aweme) {
        if (aweme == null) return;
        String id = getAwemeId(aweme);
        if (id == null) return;

        lastFeedId = id;
        awemeCache.put(id, aweme);

        if (!recentAwemes.contains(aweme)) {
            recentAwemes.add(aweme);
            if (recentAwemes.size() > 50) {
                recentAwemes.remove(0);
            }
        }

        List<Quality> qualities = extractQualitiesFromAweme(aweme);
        if (!qualities.isEmpty()) {
            qualityCache.put(id, qualities);
            boolean isPhoto = isImagePost(aweme);
            margyt().log("vqp: кэш " + (isPhoto ? "фотопост [" : "[") + id + "] " + qualities.size() + " качеств");
        }
    }

    private static boolean isImagePost(Object aweme) {
        if (aweme == null) return false;
        try {
            int type = numFromCall(aweme, "getAwemeType");
            if (type == 68 || type == 61 || type == 150) return true;

            int fType = intField(aweme, "awemeType", "mAwemeType");
            if (fType == 68 || fType == 61 || fType == 150) return true;

            Object isImg = call(aweme, "isImage");
            if (Boolean.TRUE.equals(isImg)) return true;

            Object isPhoto = call(aweme, "isPhotoMode");
            if (Boolean.TRUE.equals(isPhoto)) return true;

            Object ipi = call(aweme, "getImagePostInfo");
            if (ipi == null) ipi = objByTypeName(aweme, "ImagePostInfo");
            if (ipi != null) return true;

            Object imgs = call(aweme, "getImages");
            if (imgs instanceof List && !((List<?>) imgs).isEmpty()) return true;

            Object imgInfos = call(aweme, "getImageInfos");
            if (imgInfos instanceof List && !((List<?>) imgInfos).isEmpty()) return true;

            List<?> fImgs = listField(aweme, "images", "mImages", "imageInfos", "mImageInfos", "photoList");
            if (fImgs != null && !fImgs.isEmpty()) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static int getImageCount(Object aweme) {
        if (aweme == null) return 0;
        try {
            Object ipi = call(aweme, "getImagePostInfo");
            if (ipi == null) ipi = objByTypeName(aweme, "ImagePostInfo");
            if (ipi != null) {
                Object list = call(ipi, "getImageList");
                if (list instanceof List) return ((List<?>) list).size();
                list = call(ipi, "getImages");
                if (list instanceof List) return ((List<?>) list).size();
                List<?> fList = listField(ipi, "imageList", "mImageList", "images");
                if (fList != null) return fList.size();
            }
            Object imgs = call(aweme, "getImages");
            if (imgs instanceof List) return ((List<?>) imgs).size();
            Object imgInfos = call(aweme, "getImageInfos");
            if (imgInfos instanceof List) return ((List<?>) imgInfos).size();
            List<?> fImgs = listField(aweme, "images", "mImages", "imageInfos", "mImageInfos");
            if (fImgs != null) return fImgs.size();
        } catch (Throwable ignored) {}
        return 0;
    }

    private static List<Quality> buildPhotoModeQualityList(Object aweme) {
        List<Quality> list = new ArrayList<>();

        // Для фотопостов скачивается звук ролика (MP3), так как сервер TikTok не хранит видеоряд
        Quality audioItem = extractAudioQuality(aweme);
        if (audioItem != null) {
            list.add(new Quality(
                    audioItem.resolution,
                    audioItem.detail,
                    audioItem.urls,
                    10000,
                    true
            ));
        }

        return list;
    }

    private List<Quality> extractQualitiesFromAweme(Object aweme) {
        if (aweme == null) return Collections.emptyList();
        if (isImagePost(aweme)) {
            List<Quality> photoQualities = buildPhotoModeQualityList(aweme);
            if (!photoQualities.isEmpty()) return photoQualities;
        }
        Object video = call(aweme, "getVideo");
        if (video == null) video = objByTypeName(aweme, "Video");
        if (video == null) {
            Quality audio = extractAudioQuality(aweme);
            if (audio != null) {
                return Collections.singletonList(audio);
            }
            return Collections.emptyList();
        }
        return buildQualityList(video, aweme);
    }

    private static Quality extractAudioQuality(Object aweme) {
        if (aweme == null) return null;
        try {
            Object music = call(aweme, "getMusic");
            if (music == null) music = objByTypeName(aweme, "Music");
            if (music == null) {
                for (Field f : allFields(aweme.getClass())) {
                    if (f.getName().toLowerCase().contains("music")) {
                        try {
                            f.setAccessible(true);
                            music = f.get(aweme);
                            if (music != null) break;
                        } catch (Throwable ignored) {}
                    }
                }
            }

            List<String> audioUrls = new ArrayList<>();
            String songTitle = null;
            String songAuthor = null;
            int duration = 0;

            if (music != null) {
                songTitle = callStr(music, "getTitle");
                if (songTitle == null) songTitle = strField(music, "title", "mTitle", "musicName", "name");

                songAuthor = callStr(music, "getAuthorName");
                if (songAuthor == null) songAuthor = callStr(music, "getOwnerNickName");
                if (songAuthor == null) songAuthor = strField(music, "authorName", "author", "singer");

                duration = numFromCall(music, "getDuration");
                if (duration <= 0) duration = numFromCall(music, "getAuditionDuration");
                if (duration <= 0) duration = intField(music, "duration", "mDuration");

                Object playUrl = call(music, "getPlayUrl");
                if (playUrl != null) audioUrls.addAll(collectAllUrls(playUrl));

                Object playUrlBytevc1 = call(music, "getPlayUrlBytevc1");
                if (playUrlBytevc1 != null) audioUrls.addAll(collectAllUrls(playUrlBytevc1));

                Object dlUrl = call(music, "getDownloadUrl");
                if (dlUrl != null) audioUrls.addAll(collectAllUrls(dlUrl));

                Object playAddr = call(music, "getPlayAddr");
                if (playAddr != null) audioUrls.addAll(collectAllUrls(playAddr));

                if (audioUrls.isEmpty()) {
                    audioUrls.addAll(collectAllUrls(music));
                }
            }

            if (audioUrls.isEmpty()) {
                Object audio = call(aweme, "getAudio");
                if (audio == null) audio = strField(aweme, "audio");
                if (audio != null) {
                    audioUrls.addAll(collectAllUrls(audio));
                }
            }

            if (audioUrls.isEmpty()) return null;

            // Формируем красивое название и описание
            String title = "Звук ролика (MP3)";
            StringBuilder detail = new StringBuilder();
            if (songTitle != null && !songTitle.trim().isEmpty()) {
                String cleanTitle = songTitle.trim();
                if (cleanTitle.length() > 28) cleanTitle = cleanTitle.substring(0, 28) + "...";
                detail.append(cleanTitle);
                if (songAuthor != null && !songAuthor.trim().isEmpty() && !songTitle.contains(songAuthor)) {
                    String cleanAuthor = songAuthor.trim();
                    if (cleanAuthor.length() > 20) cleanAuthor = cleanAuthor.substring(0, 20) + "...";
                    detail.append(" • ").append(cleanAuthor);
                }
            } else {
                detail.append("Оригинальная аудиодорожка");
            }

            if (duration > 0) {
                int mins = duration / 60;
                int secs = duration % 60;
                detail.append(" · ").append(mins).append(":").append(secs < 10 ? "0" : "").append(secs);
            } else {
                detail.append(" · MP3");
            }

            return new Quality(title, detail.toString(), audioUrls, 5000, true);
        } catch (Throwable t) {
            if (sInstance != null) sInstance.margyt().log("vqp extractAudioQuality: " + t);
            return null;
        }
    }

    private static List<Quality> buildQualityList(Object video, Object aweme) {
        List<Quality> result = new ArrayList<>();

        int baseW = intFromCall(video, "getWidth");
        int baseH = intFromCall(video, "getHeight");
        int minDim = Math.min(baseW, baseH);
        int maxDim = Math.max(baseW, baseH);

        // ── 1. bitrateInfo из методов или полей ────────────────────────────────
        List<?> biList = null;
        Object mList = call(video, "getBitrateInfo");
        if (mList instanceof List) biList = (List<?>) mList;
        if (biList == null) {
            mList = call(video, "getBitrate");
            if (mList instanceof List) biList = (List<?>) mList;
        }
        if (biList == null) {
            mList = call(video, "getBitRate");
            if (mList instanceof List) biList = (List<?>) mList;
        }
        if (biList == null) {
            biList = listField(video, "bitrateInfo", "mBitrateInfo", "bitRateInfo", "bitrateList", "bitrate");
        }

        if (biList != null) {
            for (Object bi : biList) {
                try {
                    Quality q = parseBitrateInfo(bi, minDim, maxDim);
                    if (q != null) result.add(q);
                } catch (Throwable ignored) {}
            }
        }

        // ── 2. Прямые адреса из Video (чистый MP4 без водяного знака) ─────────
        String cleanLabel = (minDim > 0) ? minDim + "p (Чистый MP4)" : "Чистый MP4 (без знака)";
        addFromAddr(result, call(video, "getDownloadNoWatermarkAddr"), cleanLabel, "Официальный TikTok без водяного знака", 8500, minDim, maxDim);

        String playLabel = (minDim > 0) ? minDim + "p (Поток плеера)" : "Оригинальный поток";
        addFromAddr(result, call(video, "getPlayAddr"), playLabel, "Прямой поток плеера без водяного знака", 8000, minDim, maxDim);

        addFromAddr(result, call(video, "getDownloadAddr"), "Стандартное (с водяным знаком)", "Обычное видео со знаком TikTok", 3000, minDim, maxDim);

        // Дедупликация ТОЛЬКО по первому URL (чтобы не убирать разные качества!)
        List<Quality> dedup = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        for (Quality q : result) {
            if (q.urls.isEmpty()) continue;
            String mainUrl = q.urls.get(0);
            if (seenUrls.add(mainUrl)) {
                dedup.add(q);
            }
        }

        // ── 3. Извлечение отдельного звука ролика (MP3) ──────────────────────
        Quality audioItem = extractAudioQuality(aweme);

        // Разделяем чистые качества видео, аудиодорожку и видео с водяным знаком:
        // 1. Лучшее видео — сверху
        // 2. В раскрывающемся списке: другие качества видео -> ЗВУК РОЛИКА -> С ВОДЯНЫМ ЗНАКОМ (в самом конце)
        List<Quality> cleanVideoList = new ArrayList<>();
        Quality watermarkItem = null;

        for (Quality q : dedup) {
            if (q.resolution.contains("водяным") || q.detail.contains("со знаком")) {
                if (watermarkItem == null) watermarkItem = q;
            } else {
                cleanVideoList.add(q);
            }
        }

        cleanVideoList.sort((a, b) -> b.sortKey - a.sortKey);
        List<Quality> finalSorted = new ArrayList<>(differentiateDuplicates(cleanVideoList));

        // Отдельный звук добавляется строго ПЕРЕД вариантом с водяным знаком
        if (audioItem != null) {
            finalSorted.add(audioItem);
        }
        if (watermarkItem != null) {
            finalSorted.add(watermarkItem);
        }
        return finalSorted;
    }

    private static List<Quality> differentiateDuplicates(List<Quality> list) {
        if (list.size() <= 1) return list;

        Map<String, Integer> countMap = new HashMap<>();
        for (Quality q : list) {
            String key = extractResKey(q.resolution);
            countMap.put(key, countMap.getOrDefault(key, 0) + 1);
        }

        Map<String, Integer> indexMap = new HashMap<>();
        List<Quality> out = new ArrayList<>();

        for (Quality q : list) {
            String key = extractResKey(q.resolution);
            int total = countMap.getOrDefault(key, 0);
            if (total > 1 && !q.resolution.contains("водяным")) {
                int idx = indexMap.getOrDefault(key, 0);
                indexMap.put(key, idx + 1);

                String newRes = q.resolution;
                String newDetail = q.detail;

                if (idx == 0) {
                    if (!newDetail.contains("Высок")) newDetail = "Высокий битрейт · " + newDetail;
                } else if (idx == 1) {
                    if (!newDetail.contains("Стандарт")) newDetail = "Стандартный битрейт · " + newDetail;
                } else {
                    newRes = newRes.replace(" (576×1024)", "").replace(" (720×1280)", "") + " (Экономный)";
                    if (!newDetail.contains("Эконом")) newDetail = "Экономия памяти · " + newDetail;
                }
                out.add(new Quality(newRes, newDetail, q.urls, q.sortKey));
            } else {
                out.add(q);
            }
        }
        return out;
    }

    private static String extractResKey(String res) {
        if (res == null) return "";
        if (res.contains("1080")) return "1080";
        if (res.contains("720")) return "720";
        if (res.contains("540")) return "540";
        if (res.contains("480")) return "480";
        if (res.contains("360")) return "360";
        return res;
    }

    private static Quality parseBitrateInfo(Object bi, int baseMinDim, int baseMaxDim) {
        String gear = callStr(bi, "getGearName");
        if (gear == null) gear = strField(bi, "gearName", "gear_name", "qualityType");

        int bitrate = numFromCall(bi, "getBitrate");
        if (bitrate <= 0) bitrate = numFromCall(bi, "getBitRate");
        if (bitrate <= 0) bitrate = intField(bi, "bitrate", "mBitrate", "bitrateValue");

        Object addr = call(bi, "getPlayAddr");
        if (addr == null) addr = call(bi, "getPlayAddrBytevc1");
        if (addr == null) addr = objByTypeName(bi, "Addr", "PlayAddr");
        if (addr == null) addr = bi;

        List<String> urls = collectAllUrls(addr);
        if (urls.isEmpty()) return null;

        int w = intFromCall(addr, "getWidth");
        int h = intFromCall(addr, "getHeight");

        return makeQuality(gear, bitrate, w, h, urls, baseMinDim, baseMaxDim);
    }

    private static Quality makeQuality(String gear, int bitrate, int w, int h, List<String> urls,
                                       int baseMinDim, int baseMaxDim) {
        // Кодек
        int isBytevc1 = numFromCall(gear, "isBytevc1");
        String codec;
        if (gear != null && (gear.startsWith("bytevc2") || gear.contains("h266"))) {
            codec = "H.266 (VVC)";
        } else if (isBytevc1 == 1 || (gear != null && (gear.startsWith("bytevc1") || gear.contains("h265")))) {
            codec = "H.265 (HEVC)";
        } else {
            codec = "H.264 (AVC)";
        }

        int curMin = Math.min(w, h);
        int curMax = Math.max(w, h);
        if (curMin <= 0 && baseMinDim > 0) curMin = baseMinDim;
        if (curMax <= 0 && baseMaxDim > 0) curMax = baseMaxDim;

        // Определение названия разрешения: 1080p, 720p, 540p и т.д.
        String resName;
        int sortRank = 0;

        if (gear != null && (gear.contains("2160") || gear.contains("4k"))) {
            resName = "4K Ultra HD (2160p)"; sortRank = 2160;
        } else if (gear != null && (gear.contains("1440") || gear.contains("2k"))) {
            resName = "1440p 2K"; sortRank = 1440;
        } else if (gear != null && gear.contains("1080")) {
            resName = "1080p Full HD"; sortRank = 1080;
        } else if (gear != null && gear.contains("720")) {
            resName = "720p HD"; sortRank = 720;
        } else if (gear != null && gear.contains("540")) {
            resName = "540p"; sortRank = 540;
        } else if (gear != null && gear.contains("480")) {
            resName = "480p"; sortRank = 480;
        } else if (gear != null && gear.contains("360")) {
            resName = "360p"; sortRank = 360;
        } else if (bitrate >= 2800000) {
            resName = "1080p Full HD"; sortRank = 1080;
        } else if (bitrate >= 1400000) {
            resName = "720p HD"; sortRank = 720;
        } else if (bitrate >= 750000) {
            resName = "540p"; sortRank = 540;
        } else if (bitrate > 0) {
            resName = "480p"; sortRank = 480;
        } else if (gear != null && gear.contains("normal")) {
            resName = (curMin > 0) ? curMin + "p (Максимальное)" : "Высокое качество";
            sortRank = Math.max(curMin, 1000);
        } else if (gear != null && gear.contains("lower")) {
            resName = "540p (Среднее)"; sortRank = 540;
        } else if (gear != null && gear.contains("lowest")) {
            resName = "480p (Экономное)"; sortRank = 480;
        } else {
            resName = (curMin > 0) ? curMin + "p" : "Оригинал";
            sortRank = Math.max(curMin, 400);
        }

        if (curMin > 0 && curMax > 0 && !resName.contains("×")) {
            resName += " (" + curMin + "×" + curMax + ")";
        }

        String bitrateStr = bitrate > 0
                ? String.format("%,d kbps", bitrate / 1000)
                : "оригинальный битрейт";

        int codecBonus = codec.contains("H.265") ? 2 : codec.contains("H.266") ? 3 : 0;
        int sortKey = sortRank * 10 + codecBonus;

        return new Quality(resName, bitrateStr + " · " + codec, urls, sortKey);
    }

    private static void addFromAddr(List<Quality> out, Object urlModel, String label, String note,
                                    int baseSort, int baseMinDim, int baseMaxDim) {
        if (urlModel == null) return;
        try {
            List<String> urls = collectAllUrls(urlModel);
            if (urls.isEmpty()) return;

            int w = intFromCall(urlModel, "getWidth");
            int h = intFromCall(urlModel, "getHeight");
            int curMin = Math.min(w, h);
            int curMax = Math.max(w, h);
            if (curMin <= 0 && baseMinDim > 0) curMin = baseMinDim;
            if (curMax <= 0 && baseMaxDim > 0) curMax = baseMaxDim;

            String fullLabel = label;
            if (curMin > 0 && curMax > 0 && !label.contains("×")) {
                fullLabel = label + " (" + curMin + "×" + curMax + ")";
            }

            out.add(new Quality(fullLabel, note + " · H.264 (AVC)", urls, baseSort));
        } catch (Throwable ignored) {}
    }

    // ─────────────────────────────────────────── URL extraction & priority ────

    private static List<String> collectAllUrls(Object obj) {
        if (obj == null) return Collections.emptyList();
        Set<String> httpsDomain = new LinkedHashSet<>();
        Set<String> httpDomain  = new LinkedHashSet<>();
        Set<String> ipUrls      = new LinkedHashSet<>();

        List<String> raw = new ArrayList<>();

        Object listObj = call(obj, "getUrlList");
        if (listObj instanceof List) {
            for (Object item : (List<?>) listObj) {
                if (item instanceof String) raw.add((String) item);
            }
        }

        String uri = callStr(obj, "getUri");
        if (uri != null) raw.add(uri);

        for (Field f : allFields(obj.getClass())) {
            try {
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val instanceof String) {
                    String s = (String) val;
                    if (s.startsWith("http://") || s.startsWith("https://")) raw.add(s);
                } else if (val instanceof List) {
                    for (Object item : (List<?>) val) {
                        if (item instanceof String) {
                            String s = (String) item;
                            if (s.startsWith("http://") || s.startsWith("https://")) raw.add(s);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        for (String u : raw) {
            if (u == null || !u.startsWith("http")) continue;
            boolean isIp = false;
            try {
                String host = new URL(u).getHost();
                if (host != null && host.matches("^[0-9.]+$")) isIp = true;
            } catch (Throwable ignored) {}

            if (isIp) {
                ipUrls.add(u);
            } else if (u.startsWith("https://")) {
                httpsDomain.add(u);
            } else {
                httpDomain.add(u);
            }
        }

        List<String> sorted = new ArrayList<>();
        sorted.addAll(httpsDomain);
        sorted.addAll(httpDomain);
        sorted.addAll(ipUrls);
        return sorted;
    }

    // ─────────────────────────────────────────── helpers & reflection ─────────

    private Activity getLiveActivity(Activity fallback) {
        Activity now = margyt().screen();
        if (now != null && !now.isFinishing()) return now;
        if (fallback != null && !fallback.isFinishing()) return fallback;
        return null;
    }

    private void safeAlert(Activity activity, final String title, final String message) {
        MAIN.post(() -> {
            try {
                Activity act = getLiveActivity(activity);
                if (act == null || act.isFinishing()) return;
                new AlertDialog.Builder(act)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();
            } catch (Throwable t) {
                margyt().log("vqp safeAlert: " + t);
            }
        });
    }

    private void safeToast(Activity activity, final String message) {
        MAIN.post(() -> {
            try {
                Activity act = getLiveActivity(activity);
                if (act == null || act.isFinishing()) return;
                Toast.makeText(act, message, Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {}
        });
    }

    private static String getAwemeId(Object aweme) {
        if (aweme == null) return null;
        String id = callStr(aweme, "getAid");
        if (id != null && !id.isEmpty()) return id;
        id = callStr(aweme, "getAwemeId");
        if (id != null && !id.isEmpty()) return id;
        id = callStr(aweme, "getId");
        if (id != null && !id.isEmpty()) return id;
        return strField(aweme, "aid", "awemeId", "aweme_id", "mAwemeId", "id");
    }

    private static String getAuthorNick(Object aweme) {
        if (aweme == null) return null;
        Object author = call(aweme, "getAuthor");
        if (author != null) {
            String nick = callStr(author, "getNickname");
            if (nick != null && !nick.trim().isEmpty()) return nick.trim();
            nick = strField(author, "nickname", "mNickname", "nick_name");
            if (nick != null && !nick.trim().isEmpty()) return nick.trim();
        }
        return null;
    }

    private static String getAuthorUid(Object aweme) {
        if (aweme == null) return null;
        Object author = call(aweme, "getAuthor");
        if (author != null) {
            String uid = callStr(author, "getUniqueId");
            if (uid != null && !uid.trim().isEmpty()) return uid.trim();
            uid = strField(author, "uniqueId", "unique_id", "mUniqueId");
            if (uid != null && !uid.trim().isEmpty()) return uid.trim();
            uid = callStr(author, "getShortId");
            if (uid != null && !uid.trim().isEmpty()) return uid.trim();
            uid = strField(author, "shortId", "short_id");
            if (uid != null && !uid.trim().isEmpty()) return uid.trim();
        }
        return null;
    }

    private static String getAuthorName(Object aweme) {
        // 1. Приоритет отдаётся юзернейму (uniqueId / @handle)
        String uid = getAuthorUid(aweme);
        if (uid != null && !uid.isEmpty()) return uid;
        // 2. Если uniqueId не задан, используем отображаемое имя (nickname)
        String nick = getAuthorNick(aweme);
        if (nick != null && !nick.isEmpty()) return nick;
        return null;
    }

    private static boolean isAweme(Object obj) {
        if (obj == null) return false;
        String name = obj.getClass().getName();
        if (name.contains("Aweme")) return true;
        return call(obj, "getVideo") != null || call(obj, "getAid") != null;
    }

    private static Object call(Object obj, String method) {
        if (obj == null) return null;
        try { return obj.getClass().getMethod(method).invoke(obj); }
        catch (Throwable ignored) { return null; }
    }

    private static String callStr(Object obj, String method) {
        Object v = call(obj, method);
        return (v instanceof String) ? (String) v : null;
    }

    private static int callInt(Object obj, String method) {
        return numFromCall(obj, method);
    }

    private static int numFromCall(Object obj, String method) {
        Object v = call(obj, method);
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private static int intFromCall(Object obj, String method) {
        return numFromCall(obj, method);
    }

    private static String strField(Object obj, String... names) {
        for (String n : names) {
            try {
                Field f = deepField(obj.getClass(), n);
                if (f == null) continue;
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v instanceof String && !((String) v).isEmpty()) return (String) v;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static int intField(Object obj, String... names) {
        for (String n : names) {
            try {
                Field f = deepField(obj.getClass(), n);
                if (f == null) continue;
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    private static List<?> listField(Object obj, String... names) {
        for (String n : names) {
            try {
                Field f = deepField(obj.getClass(), n);
                if (f == null) continue;
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v instanceof List && !((List<?>) v).isEmpty()) return (List<?>) v;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object objByTypeName(Object obj, String... frags) {
        for (Field f : allFields(obj.getClass())) {
            Class<?> t = f.getType();
            if (t.isPrimitive() || t == String.class || t.isArray()) continue;
            String sn = t.getSimpleName().toLowerCase();
            for (String frag : frags) {
                if (sn.contains(frag.toLowerCase())) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(obj);
                        if (v != null) return v;
                    } catch (Throwable ignored) {}
                    break;
                }
            }
        }
        return null;
    }

    private static Field deepField(Class<?> cls, String name) {
        while (cls != null && cls != Object.class) {
            try { return cls.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) {}
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static List<Field> allFields(Class<?> cls) {
        List<Field> r = new ArrayList<>();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) r.add(f);
            cls = cls.getSuperclass();
        }
        return r;
    }

    private static String idFromIntent(Activity activity) {
        try {
            android.os.Bundle b = activity.getIntent().getExtras();
            if (b == null) return null;
            for (String k : new String[]{"aweme_id", "aid", "video_id", "item_id", "id"}) {
                String v = b.getString(k);
                if (v != null && !v.isEmpty()) return v;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static int dp(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    private static String sanitizeFileName(String s) {
        if (s == null) return "";
        // Заменяем запрещенные символы файловых систем \ / : * ? " < > | и управляющие символы
        // Буквы любых языков (кириллица, латиница и др.), цифры, точки и дефисы сохраняются!
        String clean = s.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f\r\n\t]", "_");
        clean = clean.replaceAll("_+", "_");
        clean = clean.replaceAll("^[. ]+|[. ]+$", "");
        return clean.trim();
    }

    private static String sanitize(String s) {
        return sanitizeFileName(s);
    }

    private final Set<String> loggedNames =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private void logActivityName(String name) {
        if (loggedNames.add(name)) {
            margyt().log("vqp activity: " + name);
        }
    }
}
