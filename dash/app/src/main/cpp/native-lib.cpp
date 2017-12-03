#include <android/log.h>
#include <jni.h>
#include "HSVFilter.h"

#include "patternMatch/patternMatch.h"

using namespace cv;
using namespace std;


extern "C" {

HSVFilter filter;

JNIEXPORT void JNICALL
Java_com_example_dlscj_dash_Dash_TouchCallback(JNIEnv *env, jobject instance,
                                               jint x, jint y) {
	filter.SelectColor((int)x, (int)y);
}

JNIEXPORT jboolean JNICALL
Java_com_example_dlscj_dash_Dash_HSVFilter(JNIEnv *env, jobject instance,
                                           jlong matAddrInput,
                                           jlong matAddrResult,
                                           jdoubleArray bbarr) {

	return filter.FindObj_JNI(env, instance, matAddrInput, matAddrResult, bbarr);
}

JNIEXPORT void JNICALL
Java_com_example_dlscj_dash_Dash_SetRange(JNIEnv *env, jobject instance, jintArray min, jintArray max){
    filter.SetRange(env, instance, min, max);
}

JNIEXPORT void JNICALL
Java_com_example_dlscj_dash_Dash_GetMin(JNIEnv *env, jobject instance, jintArray min){
    int len;
    len = env->GetArrayLength(min);
    jint* bb_buf = (jint *) malloc(sizeof(jint) * len);


    int* buf = filter.GetMinScalar();
    if(buf == NULL){
        bb_buf[0] = 0;
        bb_buf[1] = 0;
        bb_buf[2] = 0;
    }
    else{
        bb_buf[0] = buf[0];
        bb_buf[1] = buf[1];
        bb_buf[2] = buf[2];
    }
    env->SetIntArrayRegion(min, 0, len, bb_buf);
    free(bb_buf);
}

JNIEXPORT void JNICALL
Java_com_example_dlscj_dash_Dash_GetMax(JNIEnv *env, jobject instance, jintArray max){
    int len;
    len = env->GetArrayLength(max);
    jint* bb_buf = (jint *) malloc(sizeof(jint) * len);


    int* buf = filter.GetMaxScalar();
    if(buf == NULL){
        bb_buf[0] = 0;
        bb_buf[1] = 0;
        bb_buf[2] = 0;
    }
    else{
        bb_buf[0] = buf[0];
        bb_buf[1] = buf[1];
        bb_buf[2] = buf[2];
    }
    env->SetIntArrayRegion(max, 0, len, bb_buf);
    free(bb_buf);
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
Java_com_example_dlscj_dash_Dash_initParam2Img(JNIEnv *env,jobject instance, jint time){
    initParam2Img((int)time);
}

JNIEXPORT void JNICALL
Java_com_example_dlscj_dash_Dash_updateParam2Img(JNIEnv *env, jobject instance, jint time, jfloat velo, jfloat angle){
    updateParam2Img((int)time, (float)velo, (float)angle);
}

JNIEXPORT void JNICALL
Java_com_example_dlscj_dash_Dash_getPredicted(JNIEnv *env, jobject instance, jstring method, jfloatArray confidences){
    float* tmpConfs = NULL;
    const char* tmpChar = (*env).GetStringUTFChars(method, NULL);
    std::string metStr(tmpChar);
    getPredicted(metStr, tmpConfs);
    (*env).SetFloatArrayRegion(confidences, 0, 8, tmpConfs);
    free(tmpConfs);
}


}


