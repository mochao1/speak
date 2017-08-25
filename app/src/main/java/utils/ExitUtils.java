package utils;

import android.app.Activity;
import java.util.ArrayList;
import java.util.List;

public class ExitUtils {
public static List<Activity> activities= new ArrayList<Activity>();

public static  void finishAll(){
	for(Activity activity:activities){
		activity.finish();
	}
}
}
