package com.jrodriguezweb.pruebaenvers;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import com.jrodriguezweb.pruebaenvers.domain.Cliente;
import com.jrodriguezweb.pruebaenvers.domain.Coche;

public class PruebaEnvers {
	private EntityManagerFactory emf;
	private EntityManager em;
	private AuditReader reader;

	private final static Logger log = Logger.getLogger(PruebaEnvers.class);
	private final static int NUM_CLIENTES = 2;
	private final static int NUM_COCHES = 5;

	public static void main(String[] args) {
		PruebaEnvers pe = new PruebaEnvers();
		pe.creaDatos();
		pe.muestraDatos(0);

		pe.borraCoches(0);
		pe.muestraDatos(0);

		pe.muestraCliente(0, 1);
		
		pe.fechaAltaCliente(0);

		pe.cochesBorrados();

		pe.close();
	}

	public PruebaEnvers() {
		emf = Persistence.createEntityManagerFactory("PruebaEnvers");
		em = emf.createEntityManager();
		reader = AuditReaderFactory.get(em);
	}

	private void creaDatos() {
		TypedQuery<Long> cQuery = em.createQuery(
				"select count(*) from Cliente c", Long.class);
		Long numReg = cQuery.getSingleResult();
		if (numReg > 0) {
			log.info("Registros ya creados");
			return;
		}

		log.info("Creando registros de pruebas");
		em.getTransaction().begin();

		for (int i = 0; i < NUM_CLIENTES; i++) {
			Cliente cliente = new Cliente();
			cliente.setIdCliente(i);
			cliente.setNombre("Cliente" + i);

			for (int j = 0; j < NUM_COCHES; j++) {
				// Una matricula algo chapuza ;-)
				String letra = String.valueOf(Character.toChars(65 + j));
				final String matricula = "" + i + i + i + i + "-" + letra;

				Coche coche = new Coche();
				coche.setMatricula(matricula);
				coche.setCliente(cliente);

				cliente.addCoche(coche);

				em.persist(coche);
			}

			em.persist(cliente);
		}
		log.info("Registros de pruebas creados");

		em.getTransaction().commit();

		log.info("Fin crea datos");
	}

	public void muestraDatos(int idCliente) {
		Cliente c = em.find(Cliente.class, idCliente);
		muestraDatos(c);
	}

	private void muestraDatos(Cliente c) {
		log.info("Cliente: " + c.getNombre());
		log.info("Coches ....");
		for (Coche coche : c.getCoches()) {
			log.info("\t Coche " + coche.getMatricula());
		}
	}

	public void borraCoches(int idCliente) {
		// Obtenemos todos los coches de ese cliente
		TypedQuery<Cliente> queryCliente = em.createQuery(
				"from Cliente c where c.idCliente = :idCliente", Cliente.class);
		queryCliente.setParameter("idCliente", idCliente);

		Cliente c = queryCliente.getSingleResult();

		em.getTransaction().begin();

		List<Coche> lstCoches = c.getCoches();

		for (Iterator<Coche> itCoche = lstCoches.iterator(); itCoche.hasNext();) {
			Coche coche = itCoche.next();
			itCoche.remove();
			em.remove(coche);
		}
		em.merge(c);

		em.getTransaction().commit();

	}

	public void muestraCliente(int idCliente, Number rev) {
		Date fechaRevision = reader.getRevisionDate(rev);
		Cliente cli = reader.find(Cliente.class, idCliente, rev);
		log.info("Fecha de revision: " + fechaRevision);
		muestraDatos(cli);
	}
	
	/**
	 * Muestra cuando se dio de alta un cliente determinado
	 * @param idCliente
	 */
	public void fechaAltaCliente(int idCliente) {
		AuditQuery query = reader.createQuery().forRevisionsOfEntity(
				Cliente.class, false, true);
		query.add(AuditEntity.revisionType().eq(RevisionType.ADD));
		query.addProjection(AuditEntity.revisionNumber().min());
		query.add(AuditEntity.id().eq(idCliente));
		
		Number revision = (Number) query.getSingleResult();
		
		log.info("Cliente creado el: " + reader.getRevisionDate(revision));
	}

	/**
	 * Muestra informacion de los coches borrados
	 */
	public void cochesBorrados() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);

		AuditQuery query = reader.createQuery().forRevisionsOfEntity(
				Coche.class, false, true);
		query.add(AuditEntity.revisionType().eq(RevisionType.DEL));

		query.add(AuditEntity.revisionNumber().le(
				reader.getRevisionNumberForDate(cal.getTime())));

		List<Object[]> results = query.getResultList();
		for (Object[] obj : results) {
			Coche coche = (Coche) obj[0];
			DefaultRevisionEntity dre = (DefaultRevisionEntity) obj[1];
			RevisionType revType = (RevisionType) obj[2];

			log.info("Coche borrado " + coche.getIdCoche() + " en fecha "
					+ dre.getRevisionDate());
			// Esto da null
			// log.info("Matricula: " + coche.getMatricula());
			// Cogemos la ultima actualizacion de ese objeto anterior al DELETE
			// El motivo es que en el DELETE solo guarda el id del objeto
			AuditQuery queryAux = reader.createQuery().forRevisionsOfEntity(
					Coche.class, false, true);
			queryAux.add(AuditEntity.revisionType().ne(RevisionType.DEL));
			queryAux.addProjection(AuditEntity.revisionNumber().max());
			queryAux.add(AuditEntity.id().eq(coche.getIdCoche()));
			
			Number idUltimaRevision = (Number) queryAux.getSingleResult();
			Coche cocheAux = reader.find(Coche.class,
					coche.getIdCoche(), idUltimaRevision);
			
			log.info("Matricula: " + cocheAux.getMatricula());
		}
	}

	public void close() {
		em.close();
	}
}
