/*
package one.rewind.io.sim800;

import java.io.IOException;
import java.util.Date;

*/
/**
 * This example code demonstrates how to perform serial communications using the Raspberry Pi.
 *
 * @author Robert Savage
 *//*

public class SerialListenSMS {

	*/
/**
	 * This example program supports the following optional command arguments/options:
	 *   "--device (device-path)"                   [DEFAULT: /dev/ttyAMA0]
	 *   "--baud (baud-rate)"                       [DEFAULT: 38400]
	 *   "--data-bits (5|6|7|8)"                    [DEFAULT: 8]
	 *   "--parity (none|odd|even)"                 [DEFAULT: none]
	 *   "--stop-bits (1|2)"                        [DEFAULT: 1]
	 *   "--flow-control (none|hardware|software)"  [DEFAULT: none]
	 *
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 *//*

	public static void main(String args[]) throws InterruptedException, IOException {
		// !! ATTENTION !!
		// By default, the serial port is configured as a console port
		// for interacting with the Linux OS shell.  If you want to use
		// the serial port in a software program, you must disable the
		// OS from using this port.
		//
		// Please see this blog article for instructions on how to disable
		// the OS console for this port:
		// [url=https://www.cube-controls.com/2015/11/02/disable-serial-port-terminal-output-on-raspbian/]https://www.cube-controls.com/20 ... output-on-raspbian/[/url]

		// create Pi4J console wrapper/helper
		// (This is a utility class to abstract some of the boilerplate code)
		final Console console = new Console();

		// print program title/header
		console.title("<-- The Pi4J Project -->", "监听串口(GPIO15-Tx / GPIO16-Rx)数据并写入Memcached中");

		// allow for user to exit program using CTRL-C
		console.promptForExit();

		// create an instance of the serial communications class
		final Serial serial = SerialFactory.createInstance();
		byte [] data = new byte[1024];        //数据缓冲区

		try {
			// create serial config object
			SerialConfig config = new SerialConfig();

			System.out.println(">>>"+SerialPort.getDefaultPort());
			config.device(SerialPort.getDefaultPort())        // "/dev/ttyACM0"
					.baud(Baud._115200)
					.dataBits(DataBits._8)
					.parity(Parity.NONE)
					.stopBits(StopBits._1)
					.flowControl(FlowControl.NONE);

			// parse optional command argument options to override the default serial settings.
			if(args.length > 0){
				config = CommandArgumentParser.getSerialConfig(config, args);
			}

			// display connection details
			console.box(" Connecting to: " + config.toString(),
					" Data received on serial port will be displayed below.");

			// open the default serial device/port with the configuration settings
			serial.open(config);
			serial.flush();

			System.out.println("serial.isOpen():"+serial.isOpen());

			*/
/**初始化GPRS模块**//*

			boolean isinit = initGPRS(serial);
			long trydelay = 2000;
			while(!isinit){
				System.out.println("初始化GPRS模块不成功, 请检查模块工作状态灯, 以及SIM卡是否接触良好..."+trydelay);
				Thread.sleep(trydelay+=1000);
				isinit = initGPRS(serial);
				if(trydelay>(10*1000)){return;}        //检测10次都不成功时, 退出程序
			}

			*/
/**初始化短信参数**//*

			isinit = initGPRS_SMS(serial);
			trydelay = 2000;
			while(!isinit){
				System.out.println("初始化短信参数不成功, 请检查模块工作状态灯, 以及SIM卡是否接触良好.");
				Thread.sleep(trydelay+=1000);
				isinit = initGPRS_SMS(serial);
				if(trydelay>(10*1000)){return;}        //检测10次都不成功时, 退出程序
			}



			//每次开机时尝试读取一次存储卡中的短信
			String res = new String(sendCMD(serial, "AT+CMGL=\"ALL\""), "GBK");
			System.out.println("AT+CMGL=\"REC READ\".res:"+res);
			if(res.indexOf("OK")==-1){
				System.out.println("设置失败!");
			}

			//下面进入主程序
			System.out.println("进入短信监听程序:");
			long old_msg_delay = 60000;            //设置旧短信搜索间隔时间(毫秒),在SIM卡内存中搜索数据
			long old_msg_count = 0;                //旧短信计时器
			int index = 1;
			data = null;
			while(true){
				System.out.print(".");
				if(!serial.isOpen()){
					System.out.println("串口未打开, 退出程序");
					break;
				}

				if(old_msg_count>=old_msg_delay){
					//
					System.out.println("发送获取SIM卡内存中的所有信息的指令");
					sendCMD(serial, "AT+CMGL=\"ALL\"");
					old_msg_count = 0;
				}else{
					old_msg_count+=1000;
					//System.out.println("old_msg_count..."+old_msg_count);
				}
				if(serial.available()>0){
					while(serial.available()>0){
						data=serial.read();                //此处接收到的数据上限是1024
						//System.out.print(new String(serial.read(), "utf-8"));
					}
					serial.flush();
				}
				if(data!=null){
					//接收到数据
					String cc = new String(data, "GBK");        //处理中文
					System.out.println("cc:"+cc);
					if(cc!=null && !cc.trim().equals("")){
						//处理数据

						*/
/**
						 * 有新短信时:
						 *  +CIEV: "MESSAGE",1
						 *
						 *  +CMTI: "SM",1
						 *//*

						if(cc.indexOf("+CMTI")!=-1){
							index = getIndexFromNewSMS(cc);
							System.out.println("发现新短信.index:"+index);
							sendCMD(serial, "AT+CMGR="+index);
						}
						if(cc.indexOf("+CMGR")!=-1){
							String[] contents = getContentFromIndex(index, cc);
							System.out.println("[AT+CMGR=index]读取存在卡上的短信内容.分析后:");
							if(contents!=null){
								System.out.println("新短信内容:");
								for(String tt : contents){
									System.out.println(tt);
								}
								//保存读到的短信 -> 服务器
								if(sendDataToServer(contents)){
									//删除已读出的短信
									System.out.println("删除已读出的新短信.index:"+contents[0]);
									delSMSByIndex(serial, Integer.parseInt(contents[0]));
								}
							}else{
								System.out.println("新短信内容:null");
							}
						}

						*/
/**
						 * 查询旧短信时:
						 * AT+CMGL="ALL"
						 *
						 * +CMGL: 1,"REC READ","18620671820",,"2017/10/26,11:37:03+08",161,25
						 * just because the people11
						 * +CMGL: 2,"REC READ","18620671820",,"2017/10/26,11:37:03+08",161,25
						 * just because the people11
						 *//*

						if(cc.indexOf("CMGL:")!=-1){
							//获取第1条短信
							String[] contents = getContentFromStorageSMS(cc);
							System.out.println("[AT+CMGL=\"ALL\"]存在卡上的短信内容.分析后:");
							for(String tt : contents){
								System.out.println(tt);
							}

							//保存读到的短信
							if(sendDataToServer(contents)){
								//删除已读出的短信
								System.out.println("删除已读出的旧短信.index:"+contents[0]);
								delSMSByIndex(serial, Integer.parseInt(contents[0]));
							}
						}

					}else{
						System.out.println("data:"+new String(data));
						System.out.println("data(byte[]) 转换成 String时出错");
					}

				}

				//if(cc!=null && !cc.trim().equals(""))System.out.println(cc);
				data = null;
				Thread.sleep(1000);
			}
		}
		catch(IOException ex) {
			console.println(" ==>> SERIAL SETUP FAILED : " + ex.getMessage());
			return;
		}

	}

	*/
/**
	 * 把短信上传到服务器中
	 * @param contents    数组
	[0] - 短信位置索引
	[1] - 电话号码
	[2] - 日期+时间 2017/10/26 11:37:03+08
	[3] - 短信内容
	 * @return
	 *//*

	public static boolean sendDataToServer(String[] contents){
		System.out.println("尝试上传短信数据");
		try{
			//移除时间中的时区 +08   2017/10/26 12:38:14+08...2017-10-26 12:38:14
			String d = contents[2].substring(0,contents[2].lastIndexOf("+"));
			d = d.replace("/", "-").replace(" ", "%20");
			StringBuffer url = new StringBuffer("http://192.168.6.2:9080/webService.do?method=saveSMSBank");
			String vno = DateTimeUtil.dateToString(new Date(), "yyyyMMdd");
			vno = StringUtil.encodePassword(vno, "MD5");
			url.append("&vno=").append(vno);
			url.append("&smstype=0");
			url.append("&port=2");
			url.append("&recTime=").append(d);        //need: 2013-12-05%2014:35:20
			url.append("&phone=").append(contents[1]);
			url.append("&serialNo=0");
			url.append("&nums=0");
			url.append("&submitPort=0");
			url.append("&sendid=").append(contents[1]);
			url.append("&sendtype=0");
			url.append("&sendNo=0");

			String xx = new String(contents[3].getBytes(), "UTF-8");
			url.append("&txt=").append(java.net.URLEncoder.encode(xx, "UTF-8"));
			System.out.println("sendDataToServer().url:"+url.toString());

			String resurl = StringUtil.getContentByUrl2(url.toString());
			System.out.println("sendDataToServer().resurl:"+resurl);
			if(resurl.trim().equals("200")){
				System.out.println("数据上传成功!");
				return true;
			}else if(resurl.trim().equals("401")){
				System.out.println("这个电话号码和短信内容已上传过, 数据重复!");
				System.out.println("清除SIM卡上的短信!");
				return true;
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return false;
	}

	*/
/**
	 * 解析返回的短信内容
	 * @return
	 *//*

	public static String[] getContentFromIndex(int index, String res){
		try{
			System.out.println("尝试读取短信...getContentFromIndex.res:"+res);
			if(res.indexOf("OK")!=-1){
				System.out.println("获取短信成功,解析内容...");
				*/
/**
				 * +CMGR: "REC READ","18620671820",,"2017/10/26,11:37:03+08",161,17,0,0,"+8613010200500",145,25
				 * just because the people11
				 *
				 * +CMGR: "REC READ","18620671820",,"2017/10/26,11:37:03+08",161,17,0,0,"+8613010200500",145,25
				 * ----------------  ------------- - ---------- -----------  --- -- - - ---------------- --- --
				 * [0]               [1]           [2] [3]      [4]          [5] [6][7][8] [9]           [10][11]
				 *//*

				String[] ccs = res.split("\r\n");
				String phone = new String();
				String sendDate = new String();
				String content = new String();
				boolean isvalid = false;            //数据获取成功
				for(int i=0;i<ccs.length;i++){
					if(ccs[i].indexOf("CMGR:")!=-1){
						String[] temp1 = ccs[i].split(",");
						phone = temp1[1];
						sendDate = temp1[3]+" "+temp1[4];
						content = ccs[i+1];
						isvalid = true;
						break;        //只处理1条
					}
				}
				if(!isvalid)return null;
				//处理双引号
				phone = phone.substring(1,phone.length()-1);
				sendDate = sendDate.substring(1,sendDate.length()-1);
				String[] resu = new String[4];
				resu[0] = String.valueOf(index);
				resu[1] = phone.trim();
				resu[2] = sendDate;
				resu[3] = content;
				return resu;

			}else if(res.indexOf("CMS ERROR")!=-1){
				//CMS ERROR:321 表示所读取的内存位置出错,一般是指定位置无短信内容所致
				System.out.println("获取短信失败,错误内容...");
				return null;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	*/
/**
	 * 有新短信时,获取短信内容:
	 *  +CIEV: "MESSAGE",1
	 *
	 *  +CMTI: "SM",1
	 *
	 *  @return index    短信所在的内存位置 index
	 *//*

	public static int getIndexFromNewSMS(String cc){
		try{
			String[] ccs = cc.split("\r\n");
			for(String v : ccs){
				if(v.indexOf("CMTI: \"SM\",")!=-1){
					String c = v.substring(v.indexOf(",")+1);
					return Integer.parseInt(c);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return 0;
	}

	*/
/**
	 * 查询旧短信, 每次只抓1条:
	 * +CMGL: 4,"REC READ","106907931100",,"2017/07/02,10:15:19+08"
	 * -------- ---------- --------------  ----------- ------------
	 * [0]      [1]        [2]           [3] [4]        [5]
	 【小米】[小米移动]您2017年6月共消费1.32元，当前余额97.88元。其中：数据流量费1.32元；语音通信费0元；短/彩信费0元

	 +CMGL: 5,"REC READ","106908761100",,"2017/08/02,10:15:30+08"
	 。查询账单 [url=http://10046.mi.com]http://10046.mi.com[/url] 。
	 +CMGL: 6,"REC READ","106908761100",,"2017/08/02,10:15:30+08"
	 【小米】[小米移动]您2017年7月共消费0.81元，当前余额97.01元。其中：数据流量费0.81元；语音通信费0元；短/彩信费0元

	 OK

	 @return 数组
	 [0] - 短信位置索引
	 [1] - 电话号码
	 [2] - 日期+时间
	 [3] - 短信内容
	 *//*

	public static String[] getContentFromStorageSMS(String cc){
		String[] ccs = cc.split("\r\n");
		String smsIndex = new String();
		String phone = new String();
		String sendDate = new String();
		String content = new String();
		for(int i=0;i<ccs.length;i++){
			if(ccs[i].indexOf("CMGL:")!=-1){
				//smsIndex = Integer.parseInt(ccs[i].substring(ccs[i].indexOf("CMGL:")+5, ccs[i].indexOf(",")));
				smsIndex = ccs[i].substring(ccs[i].indexOf("CMGL:")+5, ccs[i].indexOf(","));
				String[] temp1 = ccs[i].split(",");
				phone = temp1[2];
				sendDate = temp1[4]+" "+temp1[5];
				content = ccs[i+1];
				break;        //只处理1条
			}
		}
		//处理双引号
		phone = phone.substring(1,phone.length()-1);
		sendDate = sendDate.substring(1,sendDate.length()-1);
		String[] res = new String[4];
		res[0] = smsIndex.trim();
		res[1] = phone.trim();
		res[2] = sendDate;
		res[3] = content;
		return res;
	}

	*/
/**
	 * 删除指定位置上的短信
	 * AT+CMGD=4
	 * @param index    短信索引位置
	 * @return
	 *//*

	public static boolean delSMSByIndex(Serial serial, int index){
		String res = new String(sendCMD(serial, "AT+CMGD="+index));
		System.out.println("AT+CMGD="+index+":"+res);
		//if(res.indexOf("OK")==-1){
		//    System.out.println("删除["+index+"]位置的短信失败!");
		//    return false;
		//}
		return true;
	}


	*/
/**
	 *
	 *         初始化GPRS.模块
	 *            AT        100ms 握手 / SIM卡检测等
	 *            AT+CPIN?  100ms 查询是否检测到SIM卡
	 *            AT+CSQ    100ms 信号质量测试，值为0-31,31表示最好
	 *            AT+CCID   100ms 读取SIM的CCID(SIM卡背面20位数字)，可以检测是否有SIM卡或者是否接触良好
	 *            AT+CREG?  500ms 检测是否注册网络
	 * @return
	 *//*

	public static boolean initGPRS(Serial serial){
		if(!serial.isOpen()){return false;}        //串口未准备好

		byte[] buffs = new byte[128];

		try{
			System.out.println("try send AT to module...");
			//char cmd[] = {'A', 'T'};
			//byte cmd[] = "AT".getBytes();
			//buffs = sendCMD(serial, "AT".getBytes());
			System.out.print("\r\nGPRS模块检测中...");
			buffs = sendCMD(serial, "AT");
			String res = new String(buffs);
			if(res.indexOf("OK")==-1){
				System.out.println("GPRS模块未准备好, 请检查电源和串口波特率是否正确!");
				return false;
			}
			System.out.println(" ...[正常]\r\n");
			//System.out.println("AT.res:"+res);

			System.out.print("\r\n检测SIM卡...");
			res = new String(sendCMD(serial, "AT+CPIN?"));
			if(res.indexOf("READY")==-1){
				System.out.println("SIM卡未准备好!");
				return false;
			}
			System.out.println(" ...[正常]\r\n");
			//System.out.println("AT+CPIN?.res:"+res);


			System.out.print("\r\n信号质量测试，值为0-31,31表示最好...");
			res = new String(sendCMD(serial, "AT+CSQ"));
			if(res.indexOf("ERROR")!=-1){
				System.out.println("信号质量测试检测失败");
				return false;
			}
			*/
/**
			 * +CSQ: 24,99
			 *//*

			String[] vs = res.split("\r\n");
			for(String v : vs){
				if(v.indexOf(":")!=-1){
					String x = v.substring(v.indexOf(":")+1);
					//System.out.println("x:"+x);
					System.out.println(" ...信号强度:["+x.trim()+"]\r\n");
				}
			}
			//System.out.println("AT+CSQ.res:"+res);

			res = new String(sendCMD(serial, "AT+CCID"));
			System.out.println("AT+CCID.res:"+res);

			res = new String(sendCMD(serial, "AT+CREG?"));
			System.out.println("AT+CREG.res:"+res);


		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	*/
/**
	 *
	 * 初始化GPRS.设置短信模式及短信接收参数
	 *         AT+CMGF=1                0-PDU, 1-文本格式
	 *        AT+CSDH=1
	 *        AT+CPMS="SM","SM","SM"    将信息保存在SIM卡中, SM-表示存在SIM卡上
	 *        AT+CNMI=2,1,0,1,1        收接通知,并存在指定位置(与AT+CPMS设置有关)
	 *
	 * 设置好后, 收到短信:
	 *         +CIEV: "MESSAGE",1
	 *         +CMTI: "SM",1            表示存储位置index=1
	 * @return
	 *//*

	public static boolean initGPRS_SMS(Serial serial){
		if(!serial.isOpen()){return false;}        //串口未准备好
		String res = new String();
		try{
			System.out.print("\r\n设置短信格式...");
			res = new String(sendCMD(serial, "AT+CMGF=1"));
			if(res.indexOf("OK")==-1){
				System.out.println("设置失败!");
				return false;
			}
			System.out.println(" ...[文本格式]\r\n");
			Thread.sleep(100);

			System.out.print("\r\nAT+CSDH=1...");
			res = new String(sendCMD(serial, "AT+CSDH=1"));
			if(res.indexOf("OK")==-1){
				System.out.println("设置失败!");
				return false;
			}
			System.out.println(" ...[DONE]\r\n");
			Thread.sleep(100);

			System.out.print("\r\n设置信息保存位置...");
			res = new String(sendCMD(serial, "AT+CPMS=\"SM\",\"SM\",\"SM\""));
			if(res.indexOf("OK")==-1){
				System.out.println("设置失败!");
				return false;
			}
			System.out.println(" ...[SIM卡]\r\n");
			Thread.sleep(100);

			System.out.print("\r\n收接通知,并存在指定位置...");
			res = new String(sendCMD(serial, "AT+CNMI=2,1,0,1,1"));
			if(res.indexOf("OK")==-1){
				System.out.println("设置失败!");
				return false;
			}
			System.out.println(" ...[DONE]\r\n");
			Thread.sleep(100);

		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;

	}

	//public static byte[] sendCMD(Serial serial, byte[] cmd){
	public static byte[] sendCMD(Serial serial, String cmd){
		long overtime = 10000;        //每条指令超时上限 5秒
		long timecount = 0;            //计时器
		byte[] buffs = new byte[128];

		try {
			serial.writeln(cmd+"\r");
			//serial.writeln("AT\r");
			timecount = 0;
			while(timecount<overtime){
				//System.out.print(serial.available());
				if(serial.available()>0){
					while(serial.available()>0){
						buffs = serial.read();
						//System.out.print(new String(serial.read()));
						//System.out.print(new String(buffs));
					}
					serial.flush();
					timecount = overtime;        //exit while
				}
				timecount += 100;
				Thread.sleep(100);
			}
			//System.out.println("sendCMD:"+new String(buffs));
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffs;
	}
}

// END SNIPPET: serial-snippet*/
