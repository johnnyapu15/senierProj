#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <vector>
#include <deque>

#include "patternMatch/patternMatch.h"

using namespace cv;
using namespace std;

typedef vector<vector<Point> > cvContours;

#define LOG_TAG "ball tracker"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {
    bool clicked = false;
    Mat rec = getStructuringElement(MORPH_RECT, Size(2, 2));

    //for debug
    CvPoint touched;

    //Range of Mask
    //int min_scalar[3] = {6, 178, 101};
    //int max_scalar[3] = {7, 255, 255};

    int min_scalar[3] = {179, 255, 255};
    int max_scalar[3] = {0, 0, 0};

    IplImage *image;
    Mat hsv, mask;

    //Range Parameter
    const float lower_h = 0.98;
    const float high_h = 1.02;
    const float lower_s = 0.9;
    const float high_s = 1.1;
    const float lower_v = 0.7;
    const float high_v = 1.3;


    const float erode_cnt = 7;
    const float dilate_cnt = 5;

    const int min_bb_width = 10;
    const int min_bb_height = 10;

    //opt variables
    const int opt_num = 2;
    int op_cnt = 0;
    const int thres_diff = 1000;
    bool before = false;
    Rect before_bb;

    //Pattern var


    JNIEXPORT void JNICALL
    Java_com_example_dlscj_dash_Dash_TouchCallback(JNIEnv *env, jobject instance,
                                                   jint x, jint y) {
        int h, s, v;    //hsv pixel value
        int hl, sl, vl;        //hsv lower bound
        int hh, sh, vh;        //hsv upper bound

        //Check Coordinate
        if ((int) x < 0) x = 0;
        if ((int) y < 0) y = 0;
        if ((int) x > hsv.cols) x = hsv.cols - 1;
        if ((int) y > hsv.rows) y = hsv.rows - 1;

        //for debug
        touched.x = (int) x;
        touched.y = (int) y;

        //Get hsv value
        IplImage hsv_img(hsv);
        CvScalar hsv_col = cvGet2D(&hsv_img, (int) y, (int) x);
        h = (int) hsv_col.val[0];
        s = (int) hsv_col.val[1];
        v = (int) hsv_col.val[2];

        //Calculate bounds
        hl = h * lower_h;
        sl = s * lower_s;
        vl = v * lower_v;
        LOGD("lbound : %d %d %d", hl, sl, vl);

        hh = ((h * high_h) >= 180) ? 180 : h * high_h;
        sh = ((s * high_s) >= 255) ? 255 : s * high_s;
        vh = ((v * high_v) >= 255) ? 255 : v * high_v;
        LOGD("hbound : %d %d %d", hh, sh, vh);

        //Calculate range values

        max_scalar[0] = (hh > max_scalar[0]) ? hh : max_scalar[0];
        max_scalar[1] = (sh > max_scalar[1]) ? sh : max_scalar[1];
        max_scalar[2] = (vh > max_scalar[2]) ? vh : max_scalar[2];

        min_scalar[0] = (hl < min_scalar[0]) ? hl : min_scalar[0];
        min_scalar[1] = (sl < min_scalar[1]) ? sl : min_scalar[1];
        min_scalar[2] = (vl < min_scalar[2]) ? vl : min_scalar[2];

        LOGD("min : %d %d %d", min_scalar[0], min_scalar[1], min_scalar[2]);
        LOGD("max : %d %d %d", max_scalar[0], max_scalar[1], max_scalar[2]);

        LOGD("hsv : %d %d %d", h, s, v);

        clicked = true;
    }
    bool DrawRslt(Rect& bb, JNIEnv* env, IplImage& in_img, jdouble* bb_buf, jdoubleArray bbarr, int len){
        if(bb.width > min_bb_width && bb.height > min_bb_height){
            //set before variables
            before = true;
            before_bb = bb;
            //Draw
            float centerX = bb.x + bb.width/2;
            float centerY = bb.y + bb.height/2;
            cvRectangle(&in_img, bb.tl(), bb.br(), Scalar(255), 5,5,0);
            cvCircle(&in_img, CvPoint(centerX, centerY), 2, CvScalar(255), 4);

            /*
            //for trace
            points.push_front(CvPoint(centerX, centerY));
            if(points.size() > trace_len) points.pop_back();
            */

            //Set BB and flag
            bb_buf[0] = bb.x;
            bb_buf[1] = bb.y;
            bb_buf[2] = bb.width;
            bb_buf[3] = bb.height;
            env->SetDoubleArrayRegion(bbarr, 0, len, (const jdouble*)bb_buf);
            return true;
        }
        return false;
    }

    JNIEXPORT jboolean JNICALL
    Java_com_example_dlscj_dash_Dash_HSVFilter(JNIEnv *env, jobject instance,
                                               jlong matAddrInput,
                                               jlong matAddrResult,
                                               jdoubleArray bbarr) {

        //JNI Parameter Setting
        jboolean rslt = false;
        jdouble *bb_buf;
        int len;
        len = env->GetArrayLength(bbarr);
        bb_buf = (jdouble *) malloc(sizeof(jdouble) * len);

        //JNI in, out Setting
        Mat &matInput = *(Mat *) matAddrInput;
        Mat &matResult = *(Mat *) matAddrResult;

        //points for trace
        //deque<CvPoint> points;

        IplImage in_img(matInput);
        IplImage out_img(matResult);

        //symmetry image
        cvFlip(&in_img, &out_img, 1);

        //hsv Setting
        cvtColor(matInput, hsv, COLOR_BGR2HSV);
        blur(hsv, hsv, Size(5, 5));

        //mask Setting
        if (!clicked)
            inRange(hsv,
                    Scalar(0), Scalar(0),
                    //Scalar(min_scalar[0], min_scalar[1], min_scalar[2]),
                    //Scalar(max_scalar[0], max_scalar[1], max_scalar[2]),
                    mask);
        else {
            inRange(hsv,
                    Scalar(min_scalar[0], min_scalar[1], min_scalar[2]),
                    Scalar(max_scalar[0], max_scalar[1], max_scalar[2]),
                    mask);
            LOGD("min in frame : %d %d %d", min_scalar[0], min_scalar[1], min_scalar[2]);
            LOGD("max in frame : %d %d %d", max_scalar[0], max_scalar[1], max_scalar[2]);
        }
        erode(mask, mask, rec, Point(-1, -1), erode_cnt);
        dilate(mask, mask, rec, Point(-1, -1), dilate_cnt);

        //contours Setting
        cvContours contours;
        findContours(mask, contours, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE);

        Rect bb;
        if(contours.size() > 0){
            cvContours::iterator c = contours.begin();
            if(!before){
                LOGD("begin");
                op_cnt = 0;	//Reset opt
                //Find Largest Contour
                for(cvContours::iterator it = contours.begin(); it != contours.end(); it++){
                    if((*c).size() < (*it).size()) c = it;
                }

                //Set BB and Draw
                bb = boundingRect(Mat(*c));
                rslt = DrawRslt(bb, env, in_img, bb_buf, bbarr, len);
            }
            else{
                LOGD("else start");
                Rect it_bb;
                int diffs, it_diffs;
                Point tmp;
                Point before_center = (before_bb.br() - before_bb.tl()) * 0.5;

                //Init for first iterate
                bb = boundingRect(Mat(*c));
                tmp = before_center - (Point)((bb.br() - bb.tl()) * 0.5);
                diffs = tmp.x * tmp.x + tmp.y * tmp.y;

                //Find min difference bb
                for(cvContours::iterator it = contours.begin(); it != contours.end(); it++){
                    it_bb = boundingRect(Mat(*it));
                    if(it_bb.width < min_bb_width || it_bb.height < min_bb_height)
                        continue;

                    tmp = before_center - (Point)((it_bb.br() - it_bb.tl()) * 0.5);
                    it_diffs = tmp.x*tmp.x + tmp.y*tmp.y;

                    if(diffs > it_diffs){
                        c = it;
                        bb = boundingRect(Mat(*c));
                        tmp = before_center - (Point)((bb.br() - bb.tl()) * 0.5);
                        diffs = tmp.x * tmp.x + tmp.y * tmp.y;
                    }
                }

                //If Best BB's difference is too big, choose before bb until opt_cnt
                if(diffs > thres_diff && op_cnt <= opt_num){
                    LOGD("before bb");
                    bb = before_bb;
                    op_cnt++;
                    if(op_cnt > opt_num) before = false;
                }
                else op_cnt = 0;

                rslt = DrawRslt(bb, env, in_img, bb_buf, bbarr, len);;
            }
        }
            //If fail to catch bb, wait for re-catch
        else if(op_cnt <= opt_num && before){
            LOGD("opt");
            op_cnt++;
            bb = before_bb;
            rslt = DrawRslt(bb, env, in_img, bb_buf, bbarr, len);;
        }
            //If even opt fail, reset all
        else{
            before = false;
            op_cnt = 0;
        }

        //Slow fade out
        /*if (points.size() > 0 && trace_flag) {
            points.pop_back();
            trace_flag = false;
        } else trace_flag = true;
        */
        //Draw trace
        /*deque<CvPoint>::iterator before = points.begin();
        for (deque<CvPoint>::iterator it = points.begin(); it != points.end(); it++) {
            cvLine(&in_img, (*it), (*before), CvScalar(255), 10);
            before = it;
        }*/
        free(bb_buf);

        //for debug
        cvCircle(&in_img, touched, 2, CvScalar(255), 4);

        cvarrToMat(&in_img).copyTo(matResult);

        return rslt;
    }

    JNIEXPORT void JNICALL
    Java_com_example_dlscj_dash_Dash_LineS2E(JNIEnv *env, jobject instance,
                                             jlong matAddrInput,
                                             jlong matAddrResult,
                                             jdouble s_x,
                                             jdouble s_y,
                                             jdouble e_x,
                                             jdouble e_y) {
        IplImage in_img(*(Mat *) matAddrInput);
        Mat &out_mat = *(Mat *) matAddrResult;

        cvCircle(&in_img, CvPoint((int) s_x, (int) s_y), 10, CvScalar(255), 10);
        cvCircle(&in_img, CvPoint((int) e_x, (int) e_y), 10, CvScalar(255), 10);
        cvLine(&in_img, CvPoint((int) s_x, (int) s_y), CvPoint((int) e_x, (int) e_y), CvScalar(255), 5);
        cvarrToMat(&in_img).copyTo(out_mat);
    }

    //Functions for pattern recognition

    JNIEXPORT void JNICALL
    Java_com_example_dlscj_dash_Dash_initParam2Img(JNIEnv *env,jobject instance, jint time, jstring path){
        const char* tmpChar = (*env).GetStringUTFChars(path, NULL);
        std::string tmpStr(tmpChar);
        setParam(tmpStr+"/pattern.caffemodel", tmpStr+"/pattern.prototxt");
        initParam2Img((int)time);
    }

    JNIEXPORT void JNICALL
    Java_com_example_dlscj_dash_Dash_updateParam2Img(JNIEnv *env, jobject instance, jint time, jfloat velo, jfloat angle){
        updateParam2Img((int)time, (float)velo, (float)angle);
    }

    JNIEXPORT void JNICALL
    Java_com_example_dlscj_dash_Dash_getPredicted(JNIEnv *env, jobject instance, jstring method, jfloatArray confidences){
        float* tmpConfs = new float[6];

        const char* tmpChar = (*env).GetStringUTFChars(method, NULL);
        std::string metStr(tmpChar);
        getPredicted(metStr, tmpConfs);
        (*env).SetFloatArrayRegion(confidences, 0, 6, tmpConfs);
        free(tmpConfs);
    }

    JNIEXPORT void JNICALL
    Java_com_example_dlscj_dash_Dash_getImageFromParam(JNIEnv *env, jobject instance, jlong matAddrResult){
        Mat& outMat = *(Mat*) matAddrResult;
        getImageFromParam(outMat);
    }


}


