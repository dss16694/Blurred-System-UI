package com.serajr.blurred.system.ui.hooks;

import com.android.systemui.statusbar.phone.PanelView;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_PanelView {
	
	private static boolean mBlurredNotificationPanelFadeInOut;
	
	public static void hook() {
		
		try {
			
			// onMeasure
			XposedHelpers.findAndHookMethod(PanelView.class, "onMeasure", int.class, int.class, new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// NotificationPanelView ?
					if (param.thisObject == SystemUI_NotificationPanelView.mNotificationPanelView) {
				
						// continua ?
						if (SystemUI_NotificationPanelView.mBlurredBackground == null)
							return;
						
						// erro ao criar o bitmap ?
						if (SystemUI_NotificationPanelView.mBlurredBackground.getTag().toString().equals("error")) {
							
							// torna vis�vel
							if (SystemUI_NotificationPanelView.mBlurredBackground.getAlpha() != 1.0f)
								SystemUI_NotificationPanelView.mBlurredBackground.setAlpha(1.0f);
							
							// para por aqui
							return;
							
						}
						
						// sem fade in-out ?
						if (!mBlurredNotificationPanelFadeInOut) {
							
							// torna vis�vel
							if (SystemUI_NotificationPanelView.mBlurredBackground.getAlpha() != 1.0f)
								SystemUI_NotificationPanelView.mBlurredBackground.setAlpha(1.0f);
							
							// para por aqui
							return;
							
						}
						
						// obt�m os height atual
						int height = SystemUI_NotificationPanelView.mNotificationPanelView.getMeasuredHeight();
						
						// acha o alpha
					    float alpha = 0f;
					    if (height > 0) {

					    	// divide o height do mNotificationPanelView (menor)
							// pelo height do mBlurredBackground (maior)
					    	alpha = (float) height / (float) SystemUI_NotificationPanelView.mBlurredBackground.getHeight();
					    	
					    }
					    
					    // alpha v�lido ?
					    if (alpha >= 0 && alpha <= 1) {
					    
					    	// seta o alpha
					    	SystemUI_NotificationPanelView.mBlurredBackground.setAlpha(alpha);
					    	
					    }
					}
		        }
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	public static void updatePreferences(XSharedPreferences prefs) {
		
		// atualiza
		mBlurredNotificationPanelFadeInOut = prefs.getBoolean(BlurSettings_Fragment.BLURRED_FADE_IN_OUT_PREFERENCE_KEY, BlurSettings_Fragment.BLURRED_FADE_IN_OUT_PREFERENCE_DEFAULT);
		
	}
}