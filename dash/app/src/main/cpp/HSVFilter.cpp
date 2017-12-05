#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <vector>
#include "HSVFilter.h"

using namespace
 std;
using namespace cv;

#define LOG_TAG "ball tracker"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

void HSVFilter::setBBArray_JNI(Rect& bb, JNIEnv* env, jdoubleArray& bbarr){
    jdouble *bb_buf;
    int len;
    len = env->GetArrayLength(bbarr);
    bb_buf = (jdouble *) malloc(sizeof(jdouble) * len);

	//Set BB and flag
	bb_buf[0] = bb.x;
	bb_buf[1] = bb.y;
	bb_buf[2] = bb.width;
	bb_buf[3] = bb.height;
	env->SetDoubleArrayRegion(bbarr, 0, len, (const jdouble*)bb_buf);
	free(bb_buf);
}

HSVFilter::HSVFilter(){
	Mat rec = getStructuringElement(MORPH_RECT, Size(2, 2));
}

void HSVFilter::SelectColor(int x, int y){
    int h, s, v;    //hsv pixel value
    int hl, sl, vl;        //hsv lower bound
    int hh, sh, vh;        //hsv upper bound

    //Check Coordinate
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (x > hsv.cols) x = hsv.cols - 1;
    if (y > hsv.rows) y = hsv.rows - 1;

    //for debug
    touched.x = x;
    touched.y = y;

    //Get hsv value
    IplImage hsv_img(hsv);
    CvScalar hsv_col = cvGet2D(&hsv_img, (int) y, (int) x);
    h = hsv_col.val[0];
    s = hsv_col.val[1];
    v = hsv_col.val[2];

    //Calculate bounds
    hl = (h - lower_h >= 0) ? h - lower_h : 0;
    sl = (s - lower_s >= 0) ? s - lower_s : 0;
    vl = (v - lower_v >= 0) ? v - lower_v : 0;
    LOGD("lbound : %d %d %d", hl, sl, vl);

    hh = ((h + high_h) >= 180) ? 180 : h + high_h;
    sh = ((s + high_s) >= 255) ? 255 : s + high_s;
    vh = ((v + high_v) >= 255) ? 255 : v + high_v;
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
bool HSVFilter::DrawObj_JNI(Rect& bb, JNIEnv* env, jdoubleArray& bbarr){
	if(!image) return false;
    if(bb.width > min_bb_width && bb.height > min_bb_height){
        //set before variables
        before = true;
        before_bb = bb;

        //Draw
        float centerX = bb.x + bb.width/2;
        float centerY = bb.y + bb.height/2;
        cvRectangle(image, bb.tl(), bb.br(), Scalar(0, 192, 214), 5,5,0);
        cvCircle(image, CvPoint(centerX, centerY), 2, CvScalar(0, 192, 214), 4);

        /*
        //for trace
        points.push_front(CvPoint(centerX, centerY));
        if(points.size() > trace_len) points.pop_back();
        */
		
		setBBArray_JNI(bb, env, bbarr);

        return true;
    }
    return false;
}

void HSVFilter::SetRange(JNIEnv *env, jobject instance, jintArray min, jintArray max){
    jint *min_buf, *max_buf;
    min_buf = env->GetIntArrayElements(min, NULL);
    max_buf = env->GetIntArrayElements(max, NULL);
    if(env->GetArrayLength(min) != 3) LOGD("error in Set Range, parameter min");
    if(env->GetArrayLength(max) != 3) LOGD("error in Set Range, parameter max");
    for(int i = 0; i<3; i++)
        min_scalar[i] = min_buf[i];
    for(int i = 0; i<3; i++)
        max_scalar[i] = max_buf[i];
    clicked = true;
}

jboolean HSVFilter::FindObj_JNI(JNIEnv *env, jobject instance,
					jlong matAddrInput, jlong matAddrResult,
					jdoubleArray bbarr){
    //result Setting
    jboolean rslt = false;

    //JNI in, out Setting
    Mat &matInput = *(Mat *) matAddrInput;
    Mat &matResult = *(Mat *) matAddrResult;

    //points for trace
    //deque<CvPoint> points;

    IplImage in_img(matInput);
    IplImage out_img(matResult);

	image = &in_img;

    //symmetry image
    cvFlip(image, &out_img, 1);

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
    if(contours.size() >0){
        cvContours::iterator c = contours.begin();
        if(!before){
            op_cnt = 0;	//Reset opt
            //Find Largest Contour
            for(cvContours::iterator it = contours.begin(); it != contours.end(); it++){
                if((*c).size() < (*it).size()) c = it;
            }

            //Set BB and Draw
            bb= boundingRect(Mat(*c));
            rslt = DrawObj_JNI(bb, env, bbarr);
        }
        else{
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
                bb = before_bb;
                op_cnt++;
                if(op_cnt > opt_num) before = false;
            }
            else op_cnt = 0;

            rslt = DrawObj_JNI(bb, env, bbarr);
        }
    }
        //If fail to catch bb, wait for re-catch
    else if(op_cnt <= opt_num && before){
        op_cnt++;
        bb = before_bb;
        rslt = DrawObj_JNI(bb, env, bbarr);
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

    //for debug
    cvCircle(image, touched, 2, CvScalar(255), 4);

    cvarrToMat(image).copyTo(matResult);

    return rslt;
}
