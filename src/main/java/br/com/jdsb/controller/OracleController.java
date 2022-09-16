package br.com.jdsb.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;

import br.com.jdsb.domain.Parametro;
import br.com.jdsb.domain.Trigger;
import br.com.jdsb.infra.Conexao;
import br.com.jdsb.repositories.OracleRepository;
import br.com.jdsb.repositories.ParametroRepository;
import br.com.jdsb.repositories.TriggerRepository;

@Controller
public class OracleController {
	
	@Autowired
	private Environment env;
	
	@Autowired
	private TriggerRepository triggerRepository;
	
	@Autowired
	private OracleRepository oracleRepository;
	
	@Autowired
	private ParametroRepository parametroRepository;
	
	@Autowired
	private TriggerController triggerConfig;
	
	@Autowired
	private GerarTriggerController gerarTriggerController;
	
	//@Bean
	public void executa() {
		consultaOracle();
	}
	
	private Set<String> itensNew = new HashSet<String>();
	private Set<String> itensOld = new HashSet<String>();
	
	
	
	public void consultaOracle() {
		try (Connection connection = Conexao.getConnection(env.getProperty("host"), env.getProperty("port"), env.getProperty("sid"), env.getProperty("user"), env.getProperty("password"), env.getProperty("snService"))) {
			procesaConexao(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void procesaConexao(Connection connection) throws SQLException {
		procesaConexao(connection,null,null,null);
	}
	
	public void procesaConexao(Connection connection,String triggerName,String productName,String dsVercao) throws SQLException {
		itensNew = new HashSet<String>();
		itensOld = new HashSet<String>();
		
		PreparedStatement pstmt;
		if(triggerName==null) {
		   pstmt =connection.prepareStatement("SELECT DBMS_METADATA.get_ddl ('TRIGGER', trigger_name, owner) AS TRIGGER_DETAIL,TRIGGER_NAME,TABLE_NAME from all_triggers where trigger_name = 'TRG_D_ITMVTO_ESTOQUE'");
	    }else {
	    	 pstmt =connection.prepareStatement("SELECT DBMS_METADATA.get_ddl ('TRIGGER', trigger_name, owner) AS TRIGGER_DETAIL,TRIGGER_NAME,TABLE_NAME from all_triggers where trigger_name = ? AND OWNER = 'DBAMV'");
	    	 pstmt.setString(1, triggerName);
	    }
		ResultSet rs = pstmt.executeQuery();
		if(rs==null) {
			System.out.println("OBJETO NAO ENCONTRADO: "+triggerName);
			return;
		}
		if(rs.next()) {
			String retorno = rs.getString("TRIGGER_DETAIL");
			String[] itemsNew = retorno.toLowerCase().split(":new.");
			String[] itemsOld = retorno.toLowerCase().split(":old.");
			
			for(String item:itemsNew) {
				iteraNoSetNew(trataItem(item));
			}
			
			for(String item:itemsOld) {
				iteraNoSetOld(trataItem(item));
			}
			
			String conteudoDaTrigger = retorno.trim().replace("\"", "");
			conteudoDaTrigger = conteudoDaTrigger.replace("EDITIONABLE","");
			conteudoDaTrigger = conteudoDaTrigger.replace(String.format("ALTER TRIGGER DBAMV.%s ENABLE", rs.getString("TRIGGER_NAME").toUpperCase()), "");
	
			Trigger trigger = new Trigger();
			trigger.setNmProduto(productName.toUpperCase());
			trigger.setDsVersao(dsVercao);
			trigger.setDsConteudoTrigger(conteudoDaTrigger);
			trigger.setNmTrigger(rs.getString("TRIGGER_NAME"));
			trigger.setNmTabela(rs.getString("TABLE_NAME"));
			trigger.setParametrosNew(this.itensNew);
			trigger.setParametrosOld(this.itensOld);
			List<Parametro> parametrosNovos = montaParametrosNew(trigger);
			parametroRepository.saveAll(parametrosNovos);
			
			List<Parametro> parametrosAntigos = montaParametrosOld(trigger);
			parametroRepository.saveAll(parametrosAntigos);
			
			
			trigger.setParametrosNovos(parametrosNovos);
			trigger.setParametrosAntigos(parametrosAntigos);
			String procedure = triggerConfig.processarProcedure(trigger);
			trigger.setDsProcedureGerada(procedure);
			String conteudoNovaTrigger = gerarTriggerController.montaCorpoTrigger(trigger);
			trigger.setDsConteudoNovaTrigger(conteudoNovaTrigger);
			
			//oracleRepository.compilaObjeto(trigger.getDsProcedureGerada());
			trigger.setDsStatusProcedure(oracleRepository.retornaStatusProcedure(trigger.getNmProcedure()));
			
			triggerRepository.save(trigger);
		}
		
	}
	
	public String trataItem(String item) {
	    List<Integer> posicoes = new ArrayList<>(); 
		for(int i = 0; i < item.length();i++) {
			switch (item.charAt(i)) {
			case ' ':
				posicoes.add(i);
				break;
			case ',':
				posicoes.add(i);
				break;	
			case ')':
				posicoes.add(i);
				break;	
			case ';':
				posicoes.add(i);
				break;	
			case '\n':
				posicoes.add(i);
				break;	
			case '|':
				posicoes.add(i);
				break;
			case '	':
				posicoes.add(i);
				break;		
			default:
				break;
			}
		}
		if(posicoes.isEmpty()) {
			System.out.println(item);
		}
		return item.substring(0, posicoes.get(0));
	}
	
	public List<Parametro> montaParametrosNew(Trigger trigger) {
	   List<Parametro> parametros = new ArrayList<Parametro>();
		for(String parametro:this.itensNew) {
	    	Parametro param = new Parametro();
	    	          param.setNmColuna(parametro.toUpperCase());
	    	          param.setTpParametro(oracleRepository.retornaTipoParametro(trigger.getNmTabela(), parametro));
	    	          param.setNmParametro(retornaNomeParametro(true, parametro));
	    	          if(verificaSeParametroExiste(parametros,param.getNmParametro())) {
	    	        	  if(!verificaSeColunaExiste(parametros, param.getNmColuna())) {
	    	        		  param.setNmParametro(param.getNmParametro().substring(0, 25).concat("_ALT"));
	    	        	  }
	    	          }
	    	          parametros.add(param);          
	    }
		return parametros;
	}
	
	public boolean verificaSeParametroExiste(List<Parametro> parametros,String nomeParametro) {
		boolean existe = false;
		for(Parametro parametro:parametros) {
			if(parametro.getNmParametro().equals(nomeParametro)) {
				return true;
			}
		}
		return existe;
	}
	
	public boolean verificaSeColunaExiste(List<Parametro> parametros,String nmColuna) {
		boolean existe = false;
		for(Parametro parametro:parametros) {
			if(parametro.getNmColuna().equals(nmColuna)) {
				return true;
			}
		}
		return existe;
	}
	
	public List<Parametro> montaParametrosOld(Trigger trigger) {
		   List<Parametro> parametros = new ArrayList<Parametro>();
			for(String parametro:this.itensOld) {
		    	Parametro param = new Parametro();
		    	          param.setNmColuna(parametro.toUpperCase());
		    	          param.setTpParametro(oracleRepository.retornaTipoParametro(trigger.getNmTabela(), parametro));
		    	          param.setNmParametro(retornaNomeParametro(false, parametro));
		    	          if(verificaSeParametroExiste(parametros,param.getNmParametro())) {
		    	        	  if(!verificaSeColunaExiste(parametros, param.getNmColuna())) {
		    	        		  param.setNmParametro(param.getNmParametro().substring(0, 25).concat("_ALT"));
		    	        	  }
		    	          }
		    	          parametros.add(param); 
		    }
			return parametros;
	}
	
	public void iteraNoSetNew(String itemNew) {
		if(!itemNew.isEmpty())
		itensNew.add(itemNew);
	}
	
	public void iteraNoSetOld(String itemOld) {
		if(!itemOld.isEmpty())
		itensOld.add(itemOld);
	}
	
	public String retornaNomeParametro(boolean isNew, String nomeParametro) {
		String nomeParametroSaida;
		if (isNew) {
			nomeParametroSaida = "P_NEW_".concat(nomeParametro.toUpperCase());
			if (nomeParametroSaida.length() >= 30) {
				nomeParametroSaida = nomeParametroSaida.substring(0, 29);
			}
		} else {
			nomeParametroSaida = "P_OLD_".concat(nomeParametro.toUpperCase());
			if (nomeParametroSaida.length() >= 30) {
				nomeParametroSaida = nomeParametroSaida.substring(0, 29);
			}
		}
		return nomeParametroSaida;
	}
	
	

}
