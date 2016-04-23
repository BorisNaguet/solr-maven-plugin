package com.solr.tests;

import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;

@ThreadLeakScope(Scope.NONE)
public class TestRequester extends RandomizedTest {

	private static Requester requester;

	@BeforeClass
	public static void initTest() {
		requester = new Requester();
	}
	
	@Test
	public void testPing() throws Exception {
		requester.ping();
	}
	
	@Test
	public void testAdd() throws Exception {
		requester.addDoc();
	}
}
