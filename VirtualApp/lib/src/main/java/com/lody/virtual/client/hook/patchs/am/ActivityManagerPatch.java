package com.lody.virtual.client.hook.patchs.am;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.IInterface;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.Hook;
import com.lody.virtual.client.hook.base.HookBinderDelegate;
import com.lody.virtual.client.hook.base.HookDelegate;
import com.lody.virtual.client.hook.base.Patch;
import com.lody.virtual.client.hook.base.PatchDelegate;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgHook;
import com.lody.virtual.client.hook.base.StaticHook;

import java.lang.reflect.Method;

import mirror.android.app.ActivityManagerNative;
import mirror.android.app.IActivityManager;
import mirror.android.os.ServiceManager;
import mirror.android.util.Singleton;

/**
 * @author Lody
 * @see IActivityManager
 * @see android.app.ActivityManager
 */

@Patch({StartActivity.class, StartActivityAsCaller.class,
		StartActivityAndWait.class, StartActivityWithConfig.class, StartActivityIntentSender.class,
		StartNextMatchingActivity.class, StartVoiceActivity.class,
		GetIntentSender.class, RegisterReceiver.class, GetContentProvider.class,
		GetContentProviderExternal.class,

		GetActivityClassForToken.class, GetTasks.class, GetRunningAppProcesses.class,

		StartService.class, StopService.class, StopServiceToken.class, BindService.class,
		UnbindService.class, PeekService.class, ServiceDoneExecuting.class, UnbindFinished.class,
		PublishService.class,

		HandleIncomingUser.class, SetServiceForeground.class,

		BroadcastIntent.class, GetCallingPackage.class, GrantUriPermissionFromOwner.class,
		CheckGrantUriPermission.class, GetPersistedUriPermissions.class, KillApplicationProcess.class,
		ForceStopPackage.class, AddPackageDependency.class, UpdateDeviceOwner.class,
		CrashApplication.class, GetPackageForToken.class, GetPackageForIntentSender.class,

		SetPackageAskScreenCompat.class, GetPackageAskScreenCompat.class,
		CheckPermission.class, PublishContentProviders.class, GetCurrentUser.class,
		UnstableProviderDied.class, GetCallingActivity.class, FinishActivity.class,
		GetServices.class,

		SetTaskDescription.class,})

public class ActivityManagerPatch extends PatchDelegate<HookDelegate<IInterface>> {


	@Override
	protected HookDelegate<IInterface> createHookDelegate() {
		return new HookDelegate<IInterface>() {
			@Override
			protected IInterface createInterface() {
				return ActivityManagerNative.getDefault.call();
			}
		};
	}

	@Override
	public void inject() throws Throwable {
		if (ActivityManagerNative.gDefault.type() == IActivityManager.Class) {
			ActivityManagerNative.gDefault.set(getHookDelegate().getProxyInterface());

		} else if (ActivityManagerNative.gDefault.type() == Singleton.Class) {
			Object gDefault = ActivityManagerNative.gDefault.get();
			Singleton.mInstance.set(gDefault, getHookDelegate().getProxyInterface());
		}

		HookBinderDelegate hookAMBinder = new HookBinderDelegate() {
			@Override
			protected IInterface createInterface() {
				return getHookDelegate().getBaseInterface();
			}
		};
		hookAMBinder.copyHooks(getHookDelegate());
		ServiceManager.sCache.get().put(Context.ACTIVITY_SERVICE, hookAMBinder);
	}

	@Override
	protected void onBindHooks() {
		super.onBindHooks();
		if (VirtualCore.get().isVAppProcess()) {
			addHook(new isUserRunning());
			addHook(new ReplaceCallingPkgHook("setAppLockedVerifying"));
			addHook(new StaticHook("checkUriPermission") {
				@Override
				public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
					return PackageManager.PERMISSION_GRANTED;
				}
			});
		}
	}

	private class isUserRunning extends Hook {
		@Override
		public String getName() {
			return "isUserRunning";
		}

		@Override
		public boolean beforeCall(Object who, Method method, Object... args) {
			int userId = (int) args[0];
			return userId == 0;
		}
	}

	@Override
	public boolean isEnvBad() {
		return ActivityManagerNative.getDefault.call() != getHookDelegate().getProxyInterface();
	}
}
