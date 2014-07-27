package com.serajr.blurred.system.ui.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.view.View;

import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.serajr.blurred.system.ui.Xposed;
import com.serajr.blurred.system.ui.activities.BlurSettings_Activity;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_PhoneStatusBar {
	
	private static Context mContext;
	private static Resources mResources;
	private static int mCloseHandleHeight;
	private static boolean mBlurredNotificationPanelEnabled;
	private static int mBlurredNotificationPanelScale;
	private static int mBlurredNotificationPanelRadius;
	private static int mBlurredNotificationPanelColor;
	private static boolean mAdjustmentsStartMarginPortrait;
	private static boolean mAdjustmentsStartMarginLandscape;
	
	public static void hook() {
		
		try {
			
			// makeStatusBarView
			XposedHelpers.findAndHookMethod(PhoneStatusBar.class, "makeStatusBarView", new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
					
					// obt�m os campos
					mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
					mResources = mContext.getResources();
					
					// dimens�es
					mCloseHandleHeight = mResources.getDimensionPixelSize(mResources.getIdentifier("close_handle_height", "dimen", Xposed.SYSTEM_UI_PACKAGE_NAME));
					
					// inicia o render script
					if (Utils.getAndroidAPILevel() >= 17)
						Utils.Blur.initRenderScript(mContext);
					
					// receiver
					BroadcastReceiver br = new BroadcastReceiver() {
            			
                        @Override
                        public void onReceive(Context context, Intent intent) {
                        	
                        	String action = intent.getAction();
                        	Handler handler = new Handler();
                        	
    						// -----------------------------------------------------------------------
    						// se na rota��o do celular o mod estiver habilitado e o painel expandido
    						// estiver aberto, fecha o painel expandido, for�ando o usu�rio a expandir
    						// o painel novamente para obt�r a imagem desfocada com a rota��o atual !!
    						// -----------------------------------------------------------------------
                        	if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                        		
        						// obt�m os campos
        						boolean mExpandedVisible = XposedHelpers.getBooleanField(param.thisObject, "mExpandedVisible");
        						
        						// habilitado ?
        						if (mBlurredNotificationPanelEnabled && mExpandedVisible) {
        					
        							// fecha o painel
        							XposedHelpers.callMethod(param.thisObject, "makeExpandedInvisible");
        							
        						}
                        	}
                        	
                        	// atualiza
                        	if (action.equals(BlurSettings_Fragment.BLURRED_SYSTEM_UI_UPDATE_INTENT)) {
                        		
                            	handler.postDelayed(new Runnable() {
                                	
                                    @Override
                                    public void run() {
                                    	
                                    	// recarregam as prefer�ncias
                                		Xposed.getXposedXSharedPreferences().reload();
                                		
                                		// atualizam as prefer�ncias
                                		updatePreferences();
                    					
                                    }
                                }, 100);
                        	}
                        	
                        	// mata o SystemUI.apk
                        	if (action.equals(BlurSettings_Activity.BLURRED_SYSTEM_UI_KILL_SYSTEM_UI_INTENT)) {
				
                        		// atrasa em meio segundo
                        		handler.postDelayed(new Runnable() {
                                	
                                    @Override
                                    public void run() {
                                    	
                                    	// mata
                                    	Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
                                    	
                                    }
                             	}, 100);
                        	}
                        }
                    };
                    	
                    // registra o receiver
                    IntentFilter intent = new IntentFilter();
                    intent.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
                    intent.addAction(BlurSettings_Fragment.BLURRED_SYSTEM_UI_UPDATE_INTENT);
                    intent.addAction(BlurSettings_Activity.BLURRED_SYSTEM_UI_KILL_SYSTEM_UI_INTENT);
                    mContext.registerReceiver(br, intent);
                    
                    // atualizam as prefer�ncias
            		updatePreferences();
                    
				}
			});
			
			// makeExpandedInvisible
			XposedBridge.hookMethod(
					Utils.getAndroidAPILevel() >= 19
						// >= 4.4
						? XposedHelpers.findMethodExact(PhoneStatusBar.class, "makeExpandedVisible")
						// <= 4.3
						: XposedHelpers.findMethodExact(PhoneStatusBar.class, "makeExpandedVisible", boolean.class), 
					new XC_MethodHook() {
						
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					// habilitado ?
					if (mBlurredNotificationPanelEnabled) {
							
						int left = mCloseHandleHeight;
						int top = mCloseHandleHeight;
						
						if (mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
							
							left = 0;
							
							// n�o utilizar o padding ?
							if (!mAdjustmentsStartMarginPortrait)
								top = 0;
							
						} else if (mResources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
							
							top = 0;
							
							// n�o utilizar o padding ?
							if (!mAdjustmentsStartMarginLandscape)
								left = 0;
							
						}
						
						// seta o padding da ImageView de acordo com a rota��o e escolha do usu�rio
						SystemUI_NotificationPanelView.mBlurredBackground.setPadding(left, top, 0, 0);
						
						// blur
						new BlurTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						
					}
				}
			});
			
			// makeExpandedInvisible
			XposedHelpers.findAndHookMethod(PhoneStatusBar.class, "makeExpandedInvisible", new XC_MethodHook() {
						
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// limpa a imageview e a mem�ria utilizada
					if (SystemUI_NotificationPanelView.mBlurredBackground != null &&
						SystemUI_NotificationPanelView.mBlurredBackground.getDrawable() != null) {
				
						// bitmap ?
						if (SystemUI_NotificationPanelView.mBlurredBackground.getDrawable() instanceof BitmapDrawable) {
							
							// recicla
						    Bitmap bitmap = ((BitmapDrawable) SystemUI_NotificationPanelView.mBlurredBackground.getDrawable()).getBitmap();
						    bitmap.recycle();
						    bitmap = null;
							
						}
						
						// limpa
						SystemUI_NotificationPanelView.mBlurredBackground.setImageDrawable(null);
						
					}
				}
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	private static void updatePreferences() {
		
		XSharedPreferences prefs = Xposed.getXposedXSharedPreferences();
		
		// atualiza
		mBlurredNotificationPanelEnabled = prefs.getBoolean(BlurSettings_Fragment.BLUR_ENABLED_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_ENABLED_PREFERENCE_DEFAULT);
		
		// atualiza
		mBlurredNotificationPanelScale = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_DEFAULT));
		mBlurredNotificationPanelRadius = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_DEFAULT));
		mBlurredNotificationPanelColor = prefs.getInt(BlurSettings_Fragment.BLUR_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_COLOR_PREFERENCE_DEFAULT);
		mAdjustmentsStartMarginPortrait = prefs.getBoolean(BlurSettings_Fragment.PORTRAIT_MARGIN_PREFERENCE_KEY, BlurSettings_Fragment.PORTRAIT_MARGIN_PREFERENCE_DEFAULT);
		mAdjustmentsStartMarginLandscape = prefs.getBoolean(BlurSettings_Fragment.LANDSCAPE_MARGIN_PREFERENCE_KEY, BlurSettings_Fragment.LANDSCAPE_MARGIN_PREFERENCE_DEFAULT);
		
		// atualiza
		SystemUI_BaseStatusBar.updatePreferences(prefs);
		
		// atualiza
		SystemUI_PanelView.updatePreferences(prefs);
		
		// ImageView vis�vel ?
		if (SystemUI_NotificationPanelView.mBlurredBackground != null)
			SystemUI_NotificationPanelView.mBlurredBackground.setVisibility(mBlurredNotificationPanelEnabled ? View.VISIBLE : View.GONE);
		
	}
	
	private static class BlurTask extends AsyncTask<Void, Void, Bitmap> {
		
		private int[] mScreenDimens;
		private Bitmap mScreenBitmap;
		
		@Override
		protected void onPreExecute() {
			
			long startMs = System.currentTimeMillis();
			
			// obt�m o tamamho real da tela
			mScreenDimens = Utils.getRealScreenDimensions(mContext);
			
			// n�o comentar essa linha (utilizado pelo programa !!!)
			Log.d("xx_blur_time", "onPreExecute: " + (System.currentTimeMillis() - startMs) + "ms");
			
		}
		
		@Override
		protected Bitmap doInBackground(Void... arg0) {
			
			long startMs = System.currentTimeMillis();
			
			// obt�m a screenshot
			mScreenBitmap = Utils.takeSurfaceScreenshot(mContext);
			
			// continua ?
			if (mScreenBitmap == null)
				return null;
			
			// diminui o bitmap
			Bitmap scaled = Bitmap.createScaledBitmap(mScreenBitmap, mScreenDimens[0] / mBlurredNotificationPanelScale, mScreenDimens[1] / mBlurredNotificationPanelScale, true);
			
			// blur
			if (Utils.getAndroidAPILevel() >= 17) {
			
				// 4.2.2+
				scaled = Utils.Blur.renderScriptBlur(scaled, mBlurredNotificationPanelRadius);
			
			} else {
				
				// -4.1.2
				scaled = Utils.Blur.stackBlur(scaled, mBlurredNotificationPanelRadius);
				
			}
			
			// n�o comentar essa linha (utilizado pelo programa !!!)
			Log.d("xx_blur_time", "doInBackground: " + (System.currentTimeMillis() - startMs) + "ms");
			
			return scaled;
			
		}
	
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			
			long startMs = System.currentTimeMillis();
			
			if (bitmap != null) {
				
				// -----------------------------
				// bitmap criado com sucesso !!!
				// -----------------------------
				
				// seta o bitmap j� com o efeito de desfoque
				SystemUI_NotificationPanelView.mBlurredBackground.setImageBitmap(bitmap);
				
				// seta a cor sobre o bitmap
				SystemUI_NotificationPanelView.mBlurredBackground.setColorFilter(mBlurredNotificationPanelColor, PorterDuff.Mode.MULTIPLY);
				
				// reseta o tag
				SystemUI_NotificationPanelView.mBlurredBackground.setTag("ok");
				
				// recicla o bitmap original
				mScreenBitmap.recycle();
				mScreenBitmap = null;
				
			} else {
				
				// --------------------------
				// erro ao criar o bitmap !!!
				// --------------------------
				
				// seta o filtro de cor
				SystemUI_NotificationPanelView.mBlurredBackground.setImageDrawable(new ColorDrawable(mBlurredNotificationPanelColor));
				
				// torna vis�vel
				if (SystemUI_NotificationPanelView.mBlurredBackground.getAlpha() != 1.0f)
					SystemUI_NotificationPanelView.mBlurredBackground.setAlpha(1.0f);
				
				// seta o tag de erro
				SystemUI_NotificationPanelView.mBlurredBackground.setTag("error");
				
			}
			
			// n�o comentar essa linha (utilizado pelo programa !!!)
			Log.d("xx_blur_time", "onPostExecute: " + (System.currentTimeMillis() - startMs) + "ms");
			
		}
	}
}