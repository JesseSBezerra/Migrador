package br.com.jdsb.infra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {
	private static Connection connection;

	public static Connection getConnectio() throws SQLException{
      if(connection==null || connection.isClosed()){
    	  connection = DriverManager.getConnection("jdbc:oracle:thin:@192.168.1.25:1521:prd", "mvread", "mvread");
      }
      return connection;
	}

	private static Connection connectionService;

	public static Connection getConnection(String host, String porta, String sid, String usuario, String senha,String snService) throws SQLException{
		Connection connectionService;
		String url = "jdbc:oracle:thin:@%s:%s:%s";
	    	  if(snService!=null && snService.toUpperCase().equals("S")){
	    		  url = "jdbc:oracle:thin:@%s:%s/%s";
	    	  }
	    	  connectionService = DriverManager.getConnection(String.format(url, host,porta,sid), usuario, senha);
	      return connectionService;
    }

	public static Connection obterConexaoValida() throws SQLException{
	      return connectionService;
	}
}