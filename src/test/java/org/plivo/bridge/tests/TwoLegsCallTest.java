package org.plivo.bridge.tests;
/**
 * Copyright (c) 2011 Plivo Team. See LICENSE for details.
 *  2011-08-28
 * @author Paulo reis
 */

import java.util.HashMap;
import java.util.Map;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.plivo.bridge.base.BasePlivoTest;
import org.plivo.bridge.server.GrizzlyServer.ServiceHandler;
import org.plivo.bridge.to.callback.AnsweredCallback;
import org.plivo.bridge.to.callback.CallbackStatus;
import org.plivo.bridge.to.callback.HangupCallback;
import org.plivo.bridge.to.command.ApplicationResponse;
import org.plivo.bridge.to.command.Dial;
import org.plivo.bridge.to.command.Number;
import org.plivo.bridge.to.response.CallResponse;
import org.plivo.bridge.util.PlivoTestUtils;
import org.plivo.bridge.utils.PlivoUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(enabled=false)
public class TwoLegsCallTest extends BasePlivoTest {
	
	@Test(enabled=false)
	public void initCall( ) throws Exception {
		
		ServiceHandler answerHandler = new ServiceHandler("/answered/*", 
				new HttpHandler() {
					@Override
					public void service(Request req, Response resp) throws Exception {
						System.out.println("got answered!");
						AnsweredCallback callback = AnsweredCallback.create(PlivoTestUtils.mapToSingleValue(req.getParameterMap()));
						System.out.println(callback);
						
						Assert.assertNotNull(callback.getCallUUID());
						
						ApplicationResponse ar = new ApplicationResponse();
						Dial d = new Dial();
						d.setAction(PlivoTestUtils.getCallbackUrl()+"/callbackStatus/");
						org.plivo.bridge.to.command.Number n = new Number();
						
						n.setNumber("1001");
						ar.setDial(d);
						d.setNumber(n);
						
						PlivoUtils.JAXBContext.createContext().createMarshaller().marshal(ar, resp.getOutputStream());
					}
				});
		
		ServiceHandler hangupHandler = new ServiceHandler("/hangup/*", 
				new HttpHandler() {
					@Override
					public void service(Request req, Response resp) throws Exception {
						System.out.println("got hangup!");
						HangupCallback callback = HangupCallback.create(PlivoTestUtils.mapToSingleValue(req.getParameterMap()));
						System.out.println(callback);
						
						Assert.assertNotNull(callback);
						
						resp.getWriter().write("Ok");
						resp.getWriter().flush();
						resp.getWriter().close();
						stop();
					}
				});
		
		ServiceHandler callabckHandler = new ServiceHandler("/callbackStatus/*", 
				new HttpHandler() {
					@Override
					public void service(Request req, Response resp) throws Exception {
						System.out.println("got calback status!");
						CallbackStatus callback = CallbackStatus.create(PlivoTestUtils.mapToSingleValue(req.getParameterMap()));
						System.out.println(callback);
						
						Assert.assertNotNull(callback);
						Assert.assertNotNull(callback.getHangupReason());
					}
				});
		
		
		startServer(answerHandler, hangupHandler, callabckHandler);
		
		Map<String, String> parameters = 
				new HashMap<String, String>();
		
		parameters.put("From", "9999");
		parameters.put("To", "1002");

		parameters.put("HangupUrl", PlivoTestUtils.getCallbackUrl()+"/hangup/");
		parameters.put("AnswerUrl", PlivoTestUtils.getCallbackUrl()+"/answered/");
		
		CallResponse result = client.call().single(parameters);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(result.isSuccess(), true);
		Assert.assertNotNull(result.getRequestUUID());
	}
}