package vn.edu.tdtu.motioncanvas.drawing;

import java.util.Arrays;

/**
 * Tô loang theo hàng (scanline): mỗi lần lấy một điểm, tô cả đoạn ngang liền nhau,
 * rồi đẩy điểm đầu các đoạn tô được ở hàng trên và hàng dưới vào ngăn xếp.
 */
final class FloodFill {
    private FloodFill() {
    }

    /** @return true nếu có pixel bị đổi màu. */
    static boolean fill(int[] pixels, int width, int height, int x, int y, int newColor, int tolerance) {
        if (x < 0 || y < 0 || x >= width || y >= height) return false;
        int target = pixels[y * width + x];
        if (target == newColor) return false;

        boolean[] done = new boolean[pixels.length]; // tránh lặp vô hạn khi newColor gần giống target
        IntStack stack = new IntStack();
        stack.push(y * width + x);
        boolean changed = false;
        while (!stack.isEmpty()) {
            int seed = stack.pop();
            if (!fillable(pixels, done, seed, target, tolerance)) continue;
            int row = seed / width;
            int rowStart = row * width;
            int rowEnd = rowStart + width - 1;
            int left = seed;
            while (left > rowStart && fillable(pixels, done, left - 1, target, tolerance)) left--;
            int right = seed;
            while (right < rowEnd && fillable(pixels, done, right + 1, target, tolerance)) right++;
            for (int i = left; i <= right; i++) {
                pixels[i] = newColor;
                done[i] = true;
            }
            changed = true;
            if (row > 0) pushRuns(pixels, done, stack, left - width, right - width, target, tolerance);
            if (row < height - 1) pushRuns(pixels, done, stack, left + width, right + width, target, tolerance);
        }
        return changed;
    }

    /** Đẩy điểm đầu của mỗi đoạn liên tiếp tô được trong [from, to]. */
    private static void pushRuns(int[] pixels, boolean[] done, IntStack stack, int from, int to,
                                 int target, int tolerance) {
        boolean inRun = false;
        for (int i = from; i <= to; i++) {
            boolean ok = fillable(pixels, done, i, target, tolerance);
            if (ok && !inRun) stack.push(i);
            inRun = ok;
        }
    }

    private static boolean fillable(int[] pixels, boolean[] done, int i, int target, int tolerance) {
        return !done[i] && close(pixels[i], target, tolerance);
    }

    /** Hai màu ARGB lệch nhau không quá `tolerance` ở mọi kênh. */
    private static boolean close(int a, int b, int tolerance) {
        return Math.abs((a >>> 24) - (b >>> 24)) <= tolerance
                && Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)) <= tolerance
                && Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)) <= tolerance
                && Math.abs((a & 0xFF) - (b & 0xFF)) <= tolerance;
    }

    /** Ngăn xếp int tự giãn — tránh boxing Integer cho hàng triệu pixel. */
    private static final class IntStack {
        private int[] data = new int[256];
        private int size;

        void push(int value) {
            if (size == data.length) data = Arrays.copyOf(data, size * 2);
            data[size++] = value;
        }

        int pop() {
            return data[--size];
        }

        boolean isEmpty() {
            return size == 0;
        }
    }
}
