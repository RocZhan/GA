package Hust.zp.GA;

import java.util.Random;

/**
 * 问题描述：x轴上有n个间距固定的点，但是每个点实际存在m个取值，求整个线路偏差值最小的线路
 * 解决方案：参照TSP问题，使用遗传算法搜索较优解
 * Created by Zp on 2017/11/11.
 */
public class GA {

    private int scale;//种群规模
    private int pointNum;//位置数
    private int realPointNum;//每个位置上真实的点数
    private double L;//固定间距
    private double[][][] distance;//相邻位置之间不同点的距离

    private int[][] oldPopulation;//父代种群
    private int[][] newPopulation;//子代种群
    private double[] fitness;//适应度
    private double[] ps;//单个个体概率
    private double[] pc;//个体累计概率

    private int t;//当前代数
    private int genNum;//运行代数
    private int bestGen;//最佳代数
    private int[] bestChoice;//最佳选择
    private double bestLength;//最短路径长度
    private double pCrossover;//交叉概率
    private double pMutation;//变异概率

    private double[][] x;//位置坐标
    private Random rand = new Random();

    //构造函数，传入所需参数
    public GA(int s, int n1, int n2, int g, double l, double p1, double p2) {
        scale = s;
        pointNum = n1;
        realPointNum = n2;
        genNum = g;
        L = l;
        pCrossover = p1;
        pMutation = p2;
    }

    //创造染色体，初始化种群
    private void initGA() {

        x = new GARandom(pointNum, realPointNum).randomInitPopulation();
        //求得每两个位置，所有点之间的距离
        distance = new double[pointNum - 1][realPointNum][realPointNum];
        for (int i = 0; i < pointNum - 1; i++) {
            for (int j = 0; j < realPointNum; j++) {
                for (int k = 0; k < realPointNum; k++) {
                    distance[i][j][k] = Math.abs(x[i + 1][k] - x[i][j] - L);
                }
            }
        }

        //生成父代种群的“染色体”，也就是随机选取每个位置上的点组成一个网络
        oldPopulation = new int[scale][pointNum];
        for (int i = 0; i < scale; i++) {
            for (int j = 0; j < pointNum; j++) {
                if (i < realPointNum){
                    oldPopulation[i][j] = i;
                }else{
                    oldPopulation[i][j] = rand.nextInt(realPointNum);
                }
            }
        }

        //初始换相应成员变量
        newPopulation = new int[scale][pointNum];
        ps = new double[scale];
        pc = new double[scale];
        fitness = new double[scale];
        bestChoice = new int[pointNum];
        bestLength = Integer.MAX_VALUE;
    }

    //根据先验条件求得个体适应度，并根据适应度求得单个个体的概率以及个体的累积概率
    //以便选择阶段使用
    private void evaluate(int[][] chromosome) {
        int k = 0;
        double len = 0;
        double sumFitness = 0;
        double[] tempFitness = new double[scale];

        //根据距离数组求得每条路径的适应度，也就是和固定距离L的偏差的和
        while (k < chromosome.length) {
            for (int i = 0; i < chromosome[k].length - 1; i++) {
                len += distance[i][chromosome[k][i]][chromosome[k][i + 1]];
            }
            fitness[k] = len;
            len = 0;
            k++;
        }

        //求总的适应度
        for (int i = 0; i < scale; i++) {
            tempFitness[i] = 10.0 / (fitness[i] + 1);//计算适应度，这里距离越小适应度越大，因此采用倒数的方式表示
            sumFitness += tempFitness[i];
        }

        //根据适应度，转化成相应的单个个体概率和个体的累积概率，用于后面的轮盘赌选择策略
        double tempP = 0;
        for (int i = 0; i < scale; i++) {
            ps[i] = tempFitness[i] / sumFitness;//单个个体概率
            tempP += ps[i];
            pc[i] = tempP;//个体累积概率
        }
    }

    //将上一代中通过精英选择和轮盘赌选择得到的个体复制到下一代中
    private void copyPopulation(int indexNew, int indexOld) {
        System.arraycopy(oldPopulation[indexOld], 0, newPopulation[indexNew], 0, pointNum);
    }

    //精英选择（选择上一代中fitness最好的个体，然后直接保存到下一代中）
    private void selectMaxFitness() {

        int maxId = 0;
        double tempFitness = fitness[0];

        //
        for (int i = 1; i < scale; i++) {
            if (tempFitness > fitness[i]) {
                tempFitness = fitness[i];
                maxId = i;
            }
        }

        if (bestLength > tempFitness) {
            bestLength = tempFitness;
            bestGen = t;
            System.arraycopy(oldPopulation[maxId], 0, bestChoice, 0, pointNum);

        }

        copyPopulation(0, maxId);
    }

    //轮盘赌选择策略
    private void select() {

        int j, selectId;
        double r;

        selectMaxFitness();//精英选择，保留上一代fitness最好的个体
        for (int i = 1; i < scale; i++) {
            r = rand.nextDouble();
            for (j = 0; j < scale; j++) {
                if (r <= pc[j]) {
                    break;
                }
            }
            if (j < scale) {
                selectId = j;
                copyPopulation(i, selectId);
            }
        }
    }

    //单点交叉
    private void crossover() {
        for (int k = 1; k < scale/2; k += 2) {
            double pCrossoverTemp = rand.nextDouble();
            //小于交叉概率时进行“染色体”交叉,将交叉索引(包括交叉索引处的元素)后的元素进行互换
            if (pCrossoverTemp <= pCrossover) {
                int tempCrossover;
                int indexCrossover = 1 + rand.nextInt(pointNum - 1);//排除索引值为0的情况，整体交换没有意义
                for (int i = indexCrossover; i < pointNum; i++) {
                    tempCrossover = newPopulation[k][i];
                    newPopulation[k][i] = newPopulation[k + 1][i];
                    newPopulation[k + 1][i] = tempCrossover;
                }
            }
        }
    }

    //单点变异
    private void mutation() {
        for (int i = 1; i < scale; i++) {
            double pMutationTemp = rand.nextDouble();
            if (pMutationTemp < pMutation) {
                //随机选择变异位置
                int mutationIndex = rand.nextInt(pointNum);
                //将变异位置的值保存下来
                int temp = newPopulation[i][mutationIndex];
                //获得变异值
                int mutationTemp = rand.nextInt(realPointNum);
                //确保变异值和之前的值不一样
                while (mutationTemp == temp) {
                    mutationTemp = rand.nextInt(realPointNum);
                }
                newPopulation[i][mutationIndex] = mutationTemp;
            }
        }
    }

    private void solve() {

        initGA();

        System.out.println("随机初始化点的位置：");
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[i].length; j++) {
                System.out.print(x[i][j] + ",");
            }
            System.out.println();
        }

        System.out.println("每个位置上的点和相邻位置上所有点的距离：");
        for (int i = 0; i < pointNum - 1; i++) {
            System.out.println("第" + i + "对：");
            for (int j = 0; j < realPointNum; j++) {
                for (int k = 0; k < realPointNum; k++) {
                    System.out.print(distance[i][j][k] + ",");
                }
                System.out.println();
            }
        }

        System.out.println("初始种群：");
        for (int i = 0; i < scale; i++) {
            for (int j = 0; j < pointNum; j++) {
                System.out.print(oldPopulation[i][j] + ",");
            }
            System.out.println();

        }

        //求取适应度、选择、交叉、变异，重复进行这四个步骤直到达到所要求的运行代数
        for (t = 0; t < genNum; t++) {
            evaluate(oldPopulation);
            select();
            crossover();
            mutation();

            System.arraycopy(newPopulation, 0, oldPopulation, 0, pointNum);
        }

        System.out.println("最佳代数: " + bestGen + " 最短距离: " + bestLength);
        System.out.print("最佳选择：");
        for (int i = 0; i < pointNum; i++) {
            System.out.print(bestChoice[i]);
        }
        System.out.println();

        for (int i = 0; i < pointNum; i++) {
            System.out.print(x[i][bestChoice[i]] + ",");
        }
    }

    public static void main(String[] args) {
        //s:初始种群规模 n1：位置数（题目中已经确定为5个点）
        // n2：每个位置上实际的测量点数 g：进化的代数，这里让种群进化2000代
        //l：相邻位置的固定间距，这里定为10 p1：交叉概率 p2：变异概率
        GA ga = new GA(30, 5, 10, 2000, 10, 0.8, 0.1);
        ga.solve();
    }
}
