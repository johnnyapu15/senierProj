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
    free(metStr);
    free(tmpChar);
}

//It uses index of classes for pattern validation. ex) CIRCLE -> 0, N -> 1 ...
//CLASSES : {"CIRCLE", "N", "L", "RECT", "RS", "S"};
//               0      1    2     3      4     5
JNIEXPORT jboolean JNICALL
Java_com_example_dlscj_dash_Dash_isValidPattern(JNIEnv *env, jobject instance, jint idx, jfloat threshold){
    bool vaild = true;

    int first, second = 0;
    getTwoTop(first, second, threshold);

    vaild = ((idx == first) || (idx == second));

    return valid;
}


}


