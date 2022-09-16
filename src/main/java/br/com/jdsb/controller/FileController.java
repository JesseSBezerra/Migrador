package br.com.jdsb.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;

import br.com.jdsb.domain.Trigger;
import br.com.jdsb.repositories.OracleRepository;
import br.com.jdsb.repositories.TriggerRepository;

@Controller
public class FileController {
	
	
	@Autowired
	private OracleRepository oracleRepository;
	
	@Autowired
	private OracleController configs;
	
	@Autowired
	private TriggerRepository triggerRepository;
	
	@Autowired
	private Environment env;
	
	
	private String nomeProduto;
	private String dsVersao;
	
	public void realizaProcessamento(String dsVercao) {
		this.dsVersao = dsVercao;
		List<String> produtos = new ArrayList<>();
		produtos.add("mges");
		produtos.add("mgco");
		produtos.add("mgce");
		produtos.add("acma");
		
		for(String produto:produtos) {
			atualizaBaseDeDados(produto);
		}
	}
	
	

	
	public void atualizaBaseDeDados(String produto) {
		File folder = new File(String.format(env.getProperty("caminhoArquivo"), produto) );
		this.nomeProduto = produto;
		try {
		
			triggerRepository.deleteAll();
			List<Trigger> trgs = triggerRepository.findAll();
			for(Trigger trigger:trgs) {
				if(trigger.getDsStatusProcedure().equals("INVALID") && trigger.getNmTrigger().equals("TRG_MVTO_GASES_MONIT_ST")) {
					triggerRepository.delete(trigger);
				}
			}
			processarArquivosDoDiretorio(folder);
			
			trgs = triggerRepository.findAll();
			
			processaObjetos(trgs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void processarArquivosDoDiretorio(File folder) throws Exception {
		for (File file : folder.listFiles()) {
			if (!file.isDirectory()) {
				String nomeTrigger = file.getName().replace("DBAMV_TRIGGER_", "").replaceFirst("(?i).SQL", "");
				if(nomeTrigger.contains("keep")) {
					continue;
				}
				String dados = new String(Files.readAllBytes(file.toPath()),StandardCharsets.ISO_8859_1);
				System.out.println(nomeTrigger);
				List<Trigger> lista = triggerRepository.findByNmTrigger(nomeTrigger);
				if(lista.isEmpty()) {
					oracleRepository.compilaObjeto(dados);
					configs.procesaConexao(oracleRepository.getConnection(), nomeTrigger,this.nomeProduto,this.dsVersao);
				}
			} 
		}
	}
	
	
	private String devolveProcedureTratada(Trigger trigger) {
		String corpo = trigger.getDsProcedureGerada();
		StringBuilder retorno = new StringBuilder();
		ArrayList<String> montando = new ArrayList<>();
		corpo.lines().forEach(montando::add);
		
		montando.remove(montando.size()-1);
		montando.remove(montando.size()-1);
		montando.remove(montando.size()-1);
		
		for(String linha:montando) {
			retorno.append(linha).append("\n");
		}
		
		return retorno.toString();
		
	}
	
	
	private void processaObjetos(List<Trigger> trgs) {
		
		for(Trigger trigger:trgs) {
			if(!trigger.getDsStatusProcedure().equalsIgnoreCase("INVALID")) {
				System.out.println(trigger.getNmProcedure());
				oracleRepository.compilaObjeto(devolveProcedureTratada(trigger));
				oracleRepository.encryptaObjeto(trigger.getNmProcedure());
				oracleRepository.compilaObjeto(trigger.getDsConteudoNovaTrigger());
				trigger.setDsProcedureEncriptada(oracleRepository.retornaProcedureEncripada(trigger.getNmProcedure()));
				trigger.setDsStatusTrigger(oracleRepository.retornaStatusProcedure(trigger.getNmTrigger()));
				triggerRepository.save(trigger);
			}
		}
	}
	

	@SuppressWarnings("unused")
	private void gerarArquivo(List<Trigger> trgs) {
		StringBuilder cabecalho = new StringBuilder();
		cabecalho.append("--<DS_SCRIPT>").append("\n");
		cabecalho.append("--DESCRIÇÃO..: Processo de auditorida de suprimentos DBAMV.AUDIT_SUPRIMENTOS").append("\n");
		cabecalho.append("--RESPONSAVEL: JESSE DOS SANTOS BEZERRA").append("\n");
		cabecalho.append("--DATA: 16/09/2022 13:52:00").append("\n");
		cabecalho.append("--PRODUTO....: SUPRI").append("\n");
		cabecalho.append("--APLICAÇÃO..: MGES").append("\n");
		cabecalho.append("--ARTEFATO...: DBAMV.%s (%s)").append("\n");
		cabecalho.append("--</DS_SCRIPT>").append("\n");
		cabecalho.append("--<USUARIO=DBAMV>").append("\n");
		
		StringBuilder rollbackStript = new StringBuilder();
		for(Trigger trigger:trgs) {
			rollbackStript.append("DROP TRIGGER ").append(trigger.getNmTrigger()).append("\n");;
			rollbackStript.append("/").append("\n");;
		}
		
		String caminho = "C:\\extrator\\auditoria_concluida";
		String rollback= caminho.concat("\\ROLLBACK\\");
		try {
			Files.writeString(new File(rollback.concat("ROLLBACK_SCRIPT".concat(".SQL"))).toPath(), String.format(cabecalho.toString(), "ROLLBACK_SCRIPT","PROCEDURE").concat(rollbackStript.toString()), StandardCharsets.ISO_8859_1, StandardOpenOption.CREATE);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		
		for(Trigger trigger:trgs) {
			try {
				String caminhoProcedures = caminho.concat("\\procedures\\");
				String caminhoTriggers= caminho.concat("\\triggers\\");
				String caminhoWrapper= caminho.concat("\\WRAPPED\\");
				Files.writeString(new File(caminhoProcedures.concat(trigger.getNmProcedure().concat(".SQL"))).toPath(), String.format(cabecalho.toString(), trigger.getNmProcedure(),"PROCEDURE").concat(trigger.getDsProcedureGerada()), StandardCharsets.ISO_8859_1, StandardOpenOption.CREATE);
				Files.writeString(new File(caminhoTriggers.concat(trigger.getNmTrigger().concat(".SQL"))).toPath(), String.format(cabecalho.toString(), trigger.getNmTrigger(),"TRIGGER").concat(trigger.getDsConteudoNovaTrigger()), StandardCharsets.ISO_8859_1, StandardOpenOption.CREATE);
				Files.writeString(new File(caminhoWrapper.concat(trigger.getNmProcedure().concat(".SQL"))).toPath(), String.format(cabecalho.toString(), trigger.getNmProcedure(),"PROCEDURE").concat(trigger.getDsProcedureEncriptada()), StandardCharsets.ISO_8859_1, StandardOpenOption.CREATE);

			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}
}
	
	

