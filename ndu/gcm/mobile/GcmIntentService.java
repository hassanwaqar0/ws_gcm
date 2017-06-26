package it.polito.ws_vs_gcm.ndu.gcm.mobile;

public class GcmIntentService extends IntentService {
	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;
	static final String TAG = "GCMDemo";
	public Intent[] intent_array=new Intent[5000];
	public int num_intent;
	SntpClient_ms client=new SntpClient_ms();

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		long time_ack=System.currentTimeMillis();
		if(num_intent==5000){
			num_intent=0;
		}
		intent_array[num_intent]= new Intent(intent);
		Runnable r = new MyThread(num_intent,time_ack);
		new Thread(r).start();
		num_intent++;
	}

	private void sendNotification(String msg) {
		mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,new Intent(this, MainActivity.class), 0);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_gcm)
				.setContentTitle("GCM Notification").setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
				.setContentText(msg);
		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}


	public class MyThread implements Runnable {
		int i;
		long time_ack;
	   public MyThread(Object parameter,Object parameter1) {
		   this.i=Integer.parseInt(parameter.toString());
		   this.time_ack=Long.parseLong(parameter1.toString());
	   }

	   public void run() {
		   String action = intent_array[i].getAction();
			if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
				Bundle extras = intent_array[i].getExtras();
				GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(null);
				String messageType = gcm.getMessageType(intent_array[i]);
				if (!extras.isEmpty()) {
					if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
						sendNotification("Send error: " + extras.toString());
						SharingData x = new SharingData();
						x.setData(extras.getCharSequence("MESSAGE"));
					} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
						sendNotification("Deleted messages on server: "	+ extras.toString());
						SharingData x = new SharingData();
						x.setData(extras.getCharSequence("MESSAGE"));
					} else if(GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
						Log.d("RECEIVED TIME","SN: "+extras.getCharSequence("ack_msgid")+" ,Time: "+time_ack+" Payload:"+extras.getCharSequence("ack_payload"));
						SharingData x = new SharingData();
						x.setData("PAYLOAD: "+extras.getCharSequence("ack_payload"));
						}
				}
				GcmBroadcastReceiver.completeWakefulIntent(intent_array[i]);

			} else {
				GcmBroadcastReceiver.completeWakefulIntent(intent_array[i]);
			}
		   }
	}
}
