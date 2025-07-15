package cc.wyattlabs.labs.alg;

import java.util.Arrays;

/**
 * 背包算法
 */
public class KnapsackAlg {


    /**
     * 01 背包
     * 每款物品可装0-1个
     *
     * @param cap 容量
     * @param weights 重量数组
     * @param values 价值数组
     * @return 最多可装的物品合计价值
     */
    public int once(int cap, int[] weights, int[] values) {
        final int[] dp = new int[cap+1];
        for(int i = 0, len = weights.length; i < len; i++) {
            for(int j = cap, w = weights[i], v = values[i]; j >= w; j--) {
//                System.out.println("i="+i+",w="+w+",j-w="+(j-w)+",dp["+j+"]="+dp[j]);
                dp[j] = Math.max(dp[j], dp[j-w]+v);
            }
        }
        System.out.println(getClass().getName()+".once(cap="+cap+",weights="+ Arrays.toString(weights)+
                          ",values="+Arrays.toString(weights)+
                          "):"+Arrays.toString(dp)+
                          ":"+dp[cap]);
        return dp[cap];
    }

    /**
     * 完全背包
     * 不限每款物品可装数
     *
     * @param cap 容量
     * @param weights 重量数组
     * @param values 价值数组
     * @return 最多可装的物品合计价值
     */
    public int any(int cap, int[] weights, int[] values) {
        final int[] dp = new int[cap+1];
        for(int i = 0, len = weights.length; i < len; i++) {
            for(int v = values[i],w = weights[i], j=w; j <= cap; j++) {
//                System.out.println("i="+i+",w="+w+",j-w="+(j-w)+",dp["+j+"]="+dp[j]);
                dp[j] = Math.max(dp[j], dp[j-w]+v);
            }
        }
        System.out.println(getClass().getName()+".any(cap="+cap +
                            ", weights="+Arrays.toString(weights)+
                            ",values="+Arrays.toString(weights)+
                            "):"+Arrays.toString(dp)+":"+dp[cap]);
        return dp[cap];
    }

    /**
     * 多重背包
     * 每款物品可装数有限
     *
     * @param cap 容量
     * @param weights 重量数组
     * @param values 价值数组
     * @param sizes 最多装载数
     * @return 最多可装的物品合计价值
     */
    public int multi(int cap, int[] weights, int[] values, int[] sizes) {
        final int[] dp = new int[cap+1];
        for(int i = 0, len = weights.length; i < len; i++) {
            for(int v = values[i],w = weights[i],s=sizes[i], j=0; j <= cap; j++) {
//                System.out.println("i="+i+",w="+w+",j-w="+(j-w)+",dp["+j+"]="+dp[j]);
                for (int k = 0; k*w <= j; k++) {

                }
                dp[j] = Math.max(dp[j], dp[j-w]+v);
            }
        }
        System.out.println(getClass().getName()+".size(cap="+cap +
                ", weights="+Arrays.toString(weights)+
                ",values="+Arrays.toString(weights)+
                ",sizes="+Arrays.toString(sizes)+
                "):"+Arrays.toString(dp)+":"+dp[cap]);
        return dp[cap];
    }

    public static void main(String []args) {
        final int CAP = 10;
        final int[] weights = {1,2,3,4,5,6,7,8,9}
                , values = {5,5,5,5,5,5,5,5,5}
                , sizes = {1,1,1,1,1,1,1,1,1};
        KnapsackAlg knapsack = new KnapsackAlg();
        knapsack.once(CAP, weights, values);
        knapsack.any(CAP, weights, values);
        knapsack.multi(CAP, weights, values, sizes);
    }
}
