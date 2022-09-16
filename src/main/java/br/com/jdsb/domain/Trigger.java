package br.com.jdsb.domain;

import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import lombok.Data;

@Data
@Entity(name = "MD_TRIGGER")
public class Trigger {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
	
	@Column(name = "NM_TRIGGER")
	private String nmTrigger;
	
	@Column(name = "NM_PROCEDURE")
	private String nmProcedure;
	
	@Column(name = "NM_TABELA")
	private String nmTabela;
	
	@Column(name = "DS_STATUS_PROCEDURE")
	private String dsStatusProcedure;
	
	@Column(name = "DS_STATUS_TRIGGER")
	private String dsStatusTrigger;
	
	@Column(name = "NM_PRODUTO")
	private String nmProduto;
	
	@Column(name = "DS_VERSAO")
	private String dsVersao;
	
	@Transient
	private Set<String> parametrosNew;
	
	@Transient
	private Set<String> parametrosOld;
	
	@ElementCollection
	@LazyCollection(LazyCollectionOption.FALSE)
	@CollectionTable(name = "PARAMETROS_NOVOS", joinColumns = @JoinColumn(name = "ID"))
	private List<Parametro> parametrosNovos;
	
	@ElementCollection
	@LazyCollection(LazyCollectionOption.FALSE)
	@CollectionTable(name = "PARAMETROS_ANTIGOS", joinColumns = @JoinColumn(name = "ID"))
	private List<Parametro> parametrosAntigos;
	
	@Column(name ="DS_CONTEUDO_TRIGGER")
	private String dsConteudoTrigger;
	
	@Column(name ="DS_CONTEUDO_NOVA_TRIGGER")
	private String dsConteudoNovaTrigger;
	
	@Column(name ="DS_PROCEDURE_GERADA")
	private String dsProcedureGerada;
	
	@Column(name = "TP_TEMPO_TRIGGER")
	private String tpTempoTrigger;
	
	@Column(name ="DS_PROCEDURE_ENCRIPTADA")
	private String dsProcedureEncriptada;
	

}
