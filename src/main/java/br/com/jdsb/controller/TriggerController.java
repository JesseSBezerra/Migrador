package br.com.jdsb.controller;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import br.com.jdsb.domain.Parametro;
import br.com.jdsb.domain.Trigger;
import br.com.jdsb.repositories.OracleRepository;

@Controller
public class TriggerController {
	
	
	String comandoCriacao = "CREATE OR REPLACE PROCEDURE DBAMV.%s";
	
	
	@Autowired 
	private OracleRepository repository;
	
	
	public String processarProcedure(Trigger trigger) {
		
		String cabecalho = retornaCabecalhoProcedure(trigger);
		String corpo = retornaCorpoProcedure(trigger);
		String nomeProcedure = defineNmProcedure(trigger.getNmTrigger(),trigger.getNmProduto());
		
		StringBuilder builder = new StringBuilder();
		builder.append(cabecalho);
		builder.append(corpo);
		builder.append("\n");
		builder.append("END ").append(nomeProcedure).append(";").append("\n");
		
		repository.compilaObjeto(builder.toString());
		
		builder.append("/");
		builder.append("\n");
		builder.append("GRANT EXECUTE ON ").append("DBAMV.").append(nomeProcedure).append(" TO mv2000");
		builder.append("\n");
		builder.append("/");
		
		trigger.setNmProcedure(nomeProcedure);
		
		return builder.toString();
	}
	
	
	
	public String retornaCabecalhoProcedure(Trigger trigger) {
		 String[] condicao =  trigger.getDsConteudoTrigger().split("(?i)Each Row");
		 
		 if(condicao[0].toUpperCase().contains("BEFORE")) {
			 trigger.setTpTempoTrigger("BEFORE");
		 }else {
			 trigger.setTpTempoTrigger("AFTER");
		 }
		 
		 String nomeProcedure = defineNmProcedure(trigger.getNmTrigger(),trigger.getNmProduto());
		 String cabecalho = String.format(comandoCriacao, nomeProcedure);
		 String espacamento = " ".repeat(cabecalho.length()+1);
		 StringBuilder procedure = new StringBuilder();
		 procedure.append(cabecalho);
		 
		 int qtParametrosNovos = trigger.getParametrosNovos().toArray().length;
		 int qtParametrosAntigos = trigger.getParametrosAntigos().toArray().length;
		 if(qtParametrosNovos> 0 || qtParametrosAntigos >0 ) {
		    procedure.append("(\n");
		 }
		 
		 
		 if(qtParametrosNovos>0) {
			 for(int i = 0; i < qtParametrosNovos ;i++) {
				 Parametro parametro = trigger.getParametrosNovos().get(i);
				 if(i < qtParametrosNovos-1){
					 procedure.append(espacamento).append(parametro.getNmParametro()).append(" ");
					 if(trigger.getTpTempoTrigger().equals("BEFORE")) {
						 procedure.append("IN OUT ");
					 }
					 procedure.append(parametro.getTpParametro()).append(",").append("\n");
				 }else {
					 procedure.append(espacamento).append(parametro.getNmParametro()).append(" ");
					 if(trigger.getTpTempoTrigger().equals("BEFORE")) {
						 procedure.append("IN OUT ");
					 }
					 procedure.append(parametro.getTpParametro());
				 }
			 }
		 }
		 
		 if(qtParametrosAntigos>0) {
			 if(qtParametrosNovos>0) {
			     procedure.append(",").append("\n");
			 }
			 for(int i = 0; i < qtParametrosAntigos ;i++) {
				 Parametro parametro = trigger.getParametrosAntigos().get(i);
				 if(i < qtParametrosAntigos-1){
					 procedure.append(espacamento).append(parametro.getNmParametro()).append(" ").append(parametro.getTpParametro()).append(",").append("\n");
				 }else {
					 procedure.append(espacamento).append(parametro.getNmParametro()).append(" ").append(parametro.getTpParametro());
				 }
			 }
		 }
		 if(qtParametrosNovos> 0 || qtParametrosAntigos >0 ) {
		   procedure.append(" ) ").append("\n"); 
		 }
		 procedure.append("IS").append("\n"); 
		 
		 return procedure.toString();
	}
	
	
	public String defineNmProcedure(String nmTrigger,String productName) {
		String retorno = "PRC_"+productName+"_".concat(nmTrigger.toUpperCase());
		if (retorno.length() >= 30) {
			retorno = retorno.substring(0, 29);
		}
		return retorno;
	}
	
	public String retornaCorpoProcedure(Trigger trigger) {
		String constante ="Each Row";
		int indice = trigger.getDsConteudoTrigger().toUpperCase().indexOf(constante.toUpperCase());
		String corpo = trigger.getDsConteudoTrigger().substring(indice+constante.length());
		StringBuilder retorno = new StringBuilder();
		ArrayList<String> montando = new ArrayList<>();
		corpo.lines().forEach(montando::add);
		
		int linhas = montando.toArray().length-1;
		for(int i = linhas; i > 0;i--) {
			String linha = montando.get(i);
			if(linha.contains("/")) {
				montando.remove(i);
				break;
			}
		}
		
		linhas = montando.toArray().length-1;
		int contador = 0;
		boolean triggerFinalizada = false;
		for(int i = linhas; i > 0;i--) {
			String linha = montando.get(i);
			if(linha.replace(" ", "").toLowerCase().contains("end"+trigger.getNmTrigger().toLowerCase())) {
				contador = contador +1;
				if(contador>=1) {
					triggerFinalizada = true;
					montando.remove(i);
				}
			}
		}
		
		if(!triggerFinalizada) {
	     	linhas = montando.toArray().length-1;
			for(int i = linhas; i > 0;i--) {
				String linha = montando.get(i);
				if(linha.toLowerCase().contains("end;")) {
					montando.remove(i);
					break;
				}
			}
		}
		
		//if(!triggerFinalizada) 
		//montando.remove(montando.size()-1);
	    for(String parte:montando) {
	    	retorno.append(parte).append("\n");
	    }
	   
		return normalizaCorpoProcedure(retorno.toString(), trigger);
	}
	
	public String normalizaCorpoProcedure(String corpo,Trigger trigger) {
		int indexBegin = corpo.toUpperCase().indexOf("BEGIN");
		int indexDeclare = corpo.toUpperCase().indexOf("DECLARE");
		
		boolean removeDeclare = true;
		if(indexBegin > 0 && indexDeclare > 0) {
			if(indexBegin<indexDeclare) {
				removeDeclare = false;
			}
		}
		
		String retorno;
		
		if(removeDeclare) {
		  retorno = corpo.replaceFirst("(?i)Declare", "");
		}else {
			  retorno = corpo;
		}
		String espaco ="(?i)%s ";
		String virgula ="(?i)%s,";
		String parentese ="(?i)%s\\)";
		String pontoEVirgula ="(?i)%s;";
		String pipe="(?i)%s\\|";
		String tab="(?i)%s	";
		String quebraDeLinha ="(?i)%s\n";
		for(Parametro parametro:trigger.getParametrosAntigos()) {
			retorno = retorno.replaceAll(String.format(espaco, ":old."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+" ");
			retorno = retorno.replaceAll(String.format(virgula, ":old."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+",");
			retorno = retorno.replaceAll(String.format(parentese, ":old."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+")");
			retorno = retorno.replaceAll(String.format(pontoEVirgula, ":old."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+";");
			retorno = retorno.replaceAll(String.format(quebraDeLinha, ":old."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+"\n");
			retorno = retorno.replaceAll(String.format(pipe, ":old."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+"|");
			retorno = retorno.replaceAll(String.format(tab, ":old."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+"	");

		}
		for(Parametro parametro:trigger.getParametrosNovos()) {
			retorno = retorno.replaceAll(String.format(espaco, ":New."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+" ");
			retorno = retorno.replaceAll(String.format(virgula, ":New."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+",");
			retorno = retorno.replaceAll(String.format(parentese, ":New."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+")");
			retorno = retorno.replaceAll(String.format(pontoEVirgula, ":New."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+";");
			retorno = retorno.replaceAll(String.format(quebraDeLinha, ":New."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+"\n");
			retorno = retorno.replaceAll(String.format(pipe, ":New."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+"|");
			retorno = retorno.replaceAll(String.format(tab, ":New."+parametro.getNmColuna().toLowerCase()),parametro.getNmParametro()+"	");
		}
		
		return retorno;
		
	}
	
	

}
