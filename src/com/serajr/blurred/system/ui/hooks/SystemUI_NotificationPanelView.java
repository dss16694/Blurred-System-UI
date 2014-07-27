package com.serajr.blurred.system.ui.hooks;

import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.android.systemui.statusbar.phone.NotificationPanelView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_NotificationPanelView {

	public static ImageView mBlurredBackground;
	public static NotificationPanelView mNotificationPanelView;
	
	public static void hook() {
		
		try {
			
			// onFinishInflate
			XposedHelpers.findAndHookMethod(NotificationPanelView.class, "onFinishInflate", new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				
					// guarda
					mNotificationPanelView = (NotificationPanelView) param.thisObject;
					
					// cria um novo imageview para o blurred
					mBlurredBackground = new ImageView(mNotificationPanelView.getContext());
					mBlurredBackground.setScaleType(ScaleType.CENTER_CROP);
					mBlurredBackground.setTag("ok");
					
		        	// insere na posição 0 (antes de todas as vistas)
		        	FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT); 
		        	mNotificationPanelView.addView(mBlurredBackground, 0, lp);
		        	
				}
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
}