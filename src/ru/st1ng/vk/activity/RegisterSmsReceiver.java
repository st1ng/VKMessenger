package ru.st1ng.vk.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class RegisterSmsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (!action.equals("android.provider.Telephony.SMS_RECEIVED")) {
			return;
		}
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			SmsMessage[] msgs = null;
			String str = "";
			if (bundle != null) {
				
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];
				for (int i = 0; i < msgs.length; i++) {
					msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					String addr = msgs[i].getOriginatingAddress();
					try
					{
						if(addr.equals("VKontakte"))
						{
							String message =  msgs[i].getMessageBody();
							String[] lines = message.split("\n");
							if(lines.length!=2)
								return;
							String[] codeLine = lines[0].split(" ");
							int code;
								code = Integer.parseInt(codeLine[codeLine.length-1]);
							String[] nameLine = lines[1].split(",")[0].split(" ");
							String name = nameLine[nameLine.length-1];
							Intent smsParsed = new Intent("ru.st1ng.vk.SMS_PARSE");
							smsParsed.putExtra(SignupActivity.EXTRA_CODE, code+"");
							smsParsed.putExtra(SignupActivity.EXTRA_NAME, name);
							context.sendBroadcast(smsParsed);
						}
					} catch (Exception e)
					{
						return;
					}
				}
			}
		}

	}

}
