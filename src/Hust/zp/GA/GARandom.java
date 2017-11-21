package Hust.zp.GA;

/**
 * 随机初始化初代种群
 * Created by Zp on 2017/11/11.
 */
class GARandom {
    private int pointNum,realPointNum;

    public GARandom(int pointNum,int realPointNum) {
        this.pointNum = pointNum;
        this.realPointNum = realPointNum;
    }

    public double[][] randomInitPopulation(){
        double[][] x = new double[pointNum][realPointNum];
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[0].length; j++) {
                if(i == j){
                    x[i][j] = i * 10;
                }else{
                    x[i][j] = i * 10 + (i + 1)*Math.random();
                }
            }
        }

        return x;
    }
}
