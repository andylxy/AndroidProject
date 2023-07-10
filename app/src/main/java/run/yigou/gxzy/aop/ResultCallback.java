package run.yigou.gxzy.aop;


/**
 *  作者:  zhs
 *  时间:  2023-07-10 14:40:55
 *  包名:  run.yigou.gxzy.aop
 *  类名:  ResultCallback
 *  版本:  1.0
 *  描述:
 *
*/
public interface ResultCallback {

   void onFinish(Object o, int code);

  default   void onError(Exception e){};

}
