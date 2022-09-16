package br.com.jdsb.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import br.com.jdsb.infra.Conexao;


@Service
public class OracleRepository {
	
	@Autowired
	private Environment env;
	

	public Connection getConnection() throws SQLException {
		Connection connection = Conexao.getConnection(env.getProperty("host"), env.getProperty("port"), env.getProperty("sid"), env.getProperty("user"), env.getProperty("password"), env.getProperty("snService"));
		return connection;
	}
	
	public String retornaTipoParametro(String tableName,String columnName) {
		String retorno = "";
		String consulta = "SELECT DATA_TYPE FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = UPPER(?) AND OWNER = 'DBAMV'";
		try(Connection connection = this.getConnection()){
			PreparedStatement pstmt = connection.prepareStatement(consulta);
			pstmt.setString(1, tableName);
			pstmt.setString(2, columnName);
			ResultSet rs = pstmt.executeQuery();
			if(rs.next()) {
				retorno = rs.getString("DATA_TYPE");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return retorno;
	}
	
	
	public String retornaStatusProcedure(String nmProcedure) {
		String retorno = "";
		String consulta = "SELECT STATUS FROM ALL_OBJECTS WHERE OBJECT_NAME = ?  AND OWNER = 'DBAMV'";
		try(Connection connection = this.getConnection()){
			PreparedStatement pstmt = connection.prepareStatement(consulta);
			pstmt.setString(1, nmProcedure);
			ResultSet rs = pstmt.executeQuery();
			if(rs!=null) {
				rs.next();
				retorno = rs.getString("STATUS");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return retorno;
	}
	
	
	public String retornaProcedureEncripada(String nmProcedure) {
		String retorno = "";
		String consulta = "SELECT DBMS_METADATA.get_ddl ('PROCEDURE', OBJECT_NAME, owner) AS TRIGGER_DETAIL,PROCEDURE_NAME from all_procedures where OBJECT_NAME = ?";
		try(Connection connection = this.getConnection()){
			PreparedStatement pstmt = connection.prepareStatement(consulta);
			pstmt.setString(1, nmProcedure);
			ResultSet rs = pstmt.executeQuery();
			if(rs!=null) {
				rs.next();
				retorno = rs.getString("TRIGGER_DETAIL");
				if(retorno!=null) {
					retorno = retorno.trim().replace("\"", "");
					retorno = retorno.trim().replace("\"", "");
					retorno = retorno.replace("EDITIONABLE","");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return retorno;
	}
	
	
	
	
	public void compilaObjeto(String trigger) {
		try(Connection connection = this.getConnection()){
		
			Statement pstmt = connection.createStatement();
			pstmt.execute(trigger);
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void encryptaObjeto(String nmObjeto) {
		try(Connection connection = this.getConnection()){
		
			PreparedStatement pstmt = connection.prepareStatement("{call DBAMV.PR_WRAPPED('DBAMV',?)}");
			pstmt.setString(1, nmObjeto);
			pstmt.execute();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
}
