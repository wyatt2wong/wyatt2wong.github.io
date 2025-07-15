package cc.wyattlabs.labs.alg;

import java.util.function.BiFunction;

/**
 * 排序算法
 */
public class SortAlg {
    /**
     * @return true=交换，false=不交换
     */
    public static final BiFunction<Integer, Integer, Boolean> ASC = (a, b) -> a > b;
    public static final BiFunction<Integer, Integer, Boolean> DESC = (a, b) -> a < b;

    /**
     * 冒泡排序-升序
     * @param nums 待排序整形数组
     * @param cmp 排序比较，回调返回true则交换位置
     */
    public void bubbleAsc(int[] nums, BiFunction<Integer, Integer, Boolean> cmp) {
        for (int i = 0, endIdx = nums.length - 1; i < endIdx; i++) {
            for (int j = 0, n = endIdx - i; j < n; j++) {
                final int tmp = nums[j+1];
                if (cmp.apply(nums[j], tmp)) {
                    nums[j+1] = nums[j];
                    nums[j] = tmp;
                }
            }
        }
    }

    /**
     * 快速排序-升序
     * @param nums 待排序整形数组
     * @param cmp 排序比较，回调返回true则交换位置
     */
    public void quickAsc(int[] nums, BiFunction<Integer, Integer, Boolean> cmp) {

    }

    // 选择排序
    // 插入排序
    // 希尔排序
    // 堆排序

    /**
     * 归并排序-升序
     * @param nums 待排序整形数组
     * @param cmp 排序比较，回调返回true则交换位置
     */
    public void mergeAsc(int[] nums, BiFunction<Integer, Integer, Boolean> cmp) {

    }

    // 计数排序
    // 桶排序
    // 计数排序
}
