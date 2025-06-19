package cc.wyattlabs.labs.alg;

import java.util.Arrays;

public class AlgKnapsack {
    // 01 背包
    public int zeroOne(int cap, int[] weights, int[] values) {
        final int[] dp = new int[cap+1];
        for(int i = 0, len = weights.length; i < len; i++) {
            for(int j = cap, w = weights[i], v = values[i]; j >= w; j--) {
//                System.out.println("i="+i+",w="+w+",j-w="+(j-w)+",dp["+j+"]="+dp[j]);
                dp[j] = Math.max(dp[j], dp[j-w]+v);
            }
        }
        System.out.println("knapsack01(weights="+ Arrays.toString(weights)+
                          ",values="+Arrays.toString(weights)+
                          ",cap="+cap+"):"+Arrays.toString(dp)+
                          ":"+dp[cap]);
        return dp[cap];
    }

    // 完全背包
    public int full(int cap, int[] weights, int[] values) {
        final int[] dp = new int[cap+1];
        for(int i = 0, len = weights.length; i < len; i++) {
            for(int v = values[i],w = weights[i], j=w; j <= cap; j++) {
//                System.out.println("i="+i+",w="+w+",j-w="+(j-w)+",dp["+j+"]="+dp[j]);
                dp[j] = Math.max(dp[j], dp[j-w]+v);
            }
        }
        System.out.println("knapsackFull(cap="+cap +
                            ", weights="+Arrays.toString(weights)+
                            ",values="+Arrays.toString(weights)+
                            "):"+Arrays.toString(dp)+":"+dp[cap]);
        return dp[cap];
    }

    public static void main(String []args) {
        final int CAP = 5;
        final int[] weights = {1,2,3,4,5,6,7,8,2}, values = {1,2,3,4,5,6,7,8,9};
        AlgKnapsack knapsack = new AlgKnapsack();
        knapsack.zeroOne(CAP, weights, values);
        knapsack.full(CAP, weights, values);
    }
}
