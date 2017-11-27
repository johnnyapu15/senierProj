#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <vector>

using namespace cv;
using namespace std;

typedef vector<vector<Point> > cvContours;

class HSVFilter{
private:
	Mat rec;	//Kernel For erode, delite
	bool clicked;	//check initiation or not

	//for debug
	CvPoint touched;
		
	//Range of Mask
	//int min_scalar[3] = {6, 178, 101};
	//int max_scalar[3] = {7, 255, 255};

	int min_scalar[3] = {179, 255, 255};
	int max_scalar[3] = {0, 0, 0};

	//camera capture and filtered imgs
	IplImage* image;
	Mat hsv, mask;

	//Range Parameter
	const int lower_h = 5;
	const int high_h = 5;
	const int lower_s = 20;
	const int high_s = 20;
	const int lower_v = 50;
	const int high_v = 50;

	//img filter parameter
	const float erode_cnt = 7;
	const float dilate_cnt = 5;

	//min boundary size of obj
	const int min_bb_width = 10;
	const int min_bb_height = 10;

	/* 
	 * Optimize Controll
	 * When filter miss recognizing, wait for recatch during some frames
	 */

	//opt variables
	const int opt_num = 2;
	int op_cnt = 0;
	const int thres_diff = 200;
	bool before = false;
	Rect before_bb;

	//JNI variables

	void setBBArray_JNI(Rect& bb, JNIEnv* env, jdoubleArray& bbarr);

public:
	HSVFilter();
	void SelectColor(int x, int y);
	bool DrawObj_JNI(Rect& bb, JNIEnv* env, jdoubleArray& bbarr);
	jboolean FindObj_JNI(JNIEnv *env, jobject instance,
						jlong matAddrInput, jlong matAddrResult,
						jdoubleArray bbarr);
};
