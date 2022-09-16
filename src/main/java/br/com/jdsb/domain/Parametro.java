package br.com.jdsb.domain;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;
import lombok.ToString;

@Entity(name = "MD_PARAMETRO")
@Data
@ToString
public class Parametro implements Comparable<Parametro>{
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
	
	@Column(name = "NM_PARAMETRO")
	private String nmParametro;
	
	@Column(name = "TP_PARAMETRO")
	private String tpParametro;
	
	@Column(name = "NM_COLUNA")
	private String nmColuna;

	@Override
	public int compareTo(Parametro o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parametro other = (Parametro) obj;
		return Objects.equals(nmParametro, other.nmParametro);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nmParametro);
	}
	
	
	

}
