package br.com.jdsb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import br.com.jdsb.domain.Parametro;
import br.com.jdsb.domain.Trigger;
import br.com.jdsb.repositories.TriggerRepository;

@Controller
public class GerarTriggerController {

	@Autowired
	private TriggerRepository triggerRepository;
	
	
	//@Bean
	public void geraTrigger() {
		Trigger trigger = triggerRepository.findById(18).get();
		montaCorpoTrigger(trigger);
	}
	
	
	
	public String montaCorpoTrigger(Trigger trigger) {
		String[] cabecalhoDaTrigger = trigger.getDsConteudoTrigger().split("(?i)Each Row");
		StringBuilder corpoTrigger = new StringBuilder();
		corpoTrigger.append(cabecalhoDaTrigger[0]).append("Each Row");
		corpoTrigger.append("\n");
		corpoTrigger.append("BEGIN");
		corpoTrigger.append("\n");
		montaParametrosProcedure(corpoTrigger, trigger);
		return corpoTrigger.toString();
	}
	
	public void montaParametrosProcedure(StringBuilder corpoTrigger,Trigger trigger) {
		int contadorParametrosNovos = trigger.getParametrosNovos().toArray().length;
		int contadorParametrosAntigos = trigger.getParametrosAntigos().toArray().length;
		String cabecalhoProcedure = ("DBAMV.").concat(trigger.getNmProcedure()).concat("(");
		corpoTrigger.append("DBAMV.").append(trigger.getNmProcedure()).append("(");
		corpoTrigger.append("\n");
		for(int i = 0; i<contadorParametrosNovos;i++) {
			Parametro parametro = trigger.getParametrosNovos().get(i);
			corpoTrigger.append(" ".repeat(cabecalhoProcedure.length())).append(parametro.getNmParametro()).append(" =>").append(" :NEW.").append(parametro.getNmColuna());
			if(i<contadorParametrosNovos-1) {
			    corpoTrigger.append(",").append("\n");
			}else {
				if(contadorParametrosAntigos>0) {
					corpoTrigger.append(",").append("\n");
				}else {
					corpoTrigger.append(");").append("\n");
				}
			}
		}
		
		for(int i = 0; i<contadorParametrosAntigos;i++) {
			Parametro parametro = trigger.getParametrosAntigos().get(i);
			corpoTrigger.append(" ".repeat(cabecalhoProcedure.length())).append(parametro.getNmParametro()).append(" =>").append(" :OLD.").append(parametro.getNmColuna());
			if(i<contadorParametrosAntigos-1) {
			    corpoTrigger.append(",").append("\n");
			}else {
				corpoTrigger.append(");").append("\n");
			}
		}
		
	   corpoTrigger.append("END").append(" ").append(trigger.getNmTrigger()).append(";").append("\n");
		
	}
}
