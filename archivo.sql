-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: escuela-empresa
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `administrador`
--

DROP TABLE IF EXISTS `administrador`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `administrador` (
  `id_Ad` int(11) NOT NULL AUTO_INCREMENT,
  `nombres` varchar(100) DEFAULT NULL,
  `apellidos` varchar(100) DEFAULT NULL,
  `ci` varchar(25) DEFAULT NULL,
  `telefono` varchar(35) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `cargo` varchar(45) DEFAULT NULL,
  `id_Usr` int(11) NOT NULL,
  PRIMARY KEY (`id_Ad`),
  KEY `fk_Administrador_Usuario1_idx` (`id_Usr`),
  CONSTRAINT `fk_Administrador_Usuario1` FOREIGN KEY (`id_Usr`) REFERENCES `usuario` (`id_Usr`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `administrador`
--

LOCK TABLES `administrador` WRITE;
/*!40000 ALTER TABLE `administrador` DISABLE KEYS */;
INSERT INTO `administrador` VALUES (1,'admin','root','1111111','1111111111','admin@ctn.com','Administrativo',1),(2,'Coordinador1','hola','1111112','1111111112','cordi1@ctn.com','Coordinador',2),(3,'Kevin','Admin','1234567','0981000000','kevindure4544@gmail.com','administrativo',4),(4,'Lamine','Messi',NULL,NULL,'coordiEik@ctn.com','Coordinador',5),(5,'Admin','Prueba',NULL,NULL,'admin3@ctn.com','Administrativo',7),(6,'Coordi','Prueba',NULL,NULL,'coordi2@ctn.com','Coordinador',8),(7,'Silvia','Duarte','3874512','0971456782','admin4@ctn.com','administrativo',18),(9,'Gustavo','Meza','3874514','0981447209','coord.construcciones@ctn.com','coordinador',20),(10,'Norma','Cáceres','3874515','0972618053','coord.electricidad@ctn.com','coordinador',21),(11,'Aníbal','Rojas','3874516','0985330941','coord.electromecanica@ctn.com','coordinador',22),(12,'Vicente','Paredes','3874517','0976125884','coord.automotriz@ctn.com','coordinador',23),(13,'Mirta','Aquino','3874518','0991502736','coord.quimica@ctn.com','coordinador',24);
/*!40000 ALTER TABLE `administrador` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alumno`
--

DROP TABLE IF EXISTS `alumno`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `alumno` (
  `id_Al` int(11) NOT NULL AUTO_INCREMENT,
  `nombres` varchar(100) DEFAULT NULL,
  `apellidos` varchar(100) DEFAULT NULL,
  `ci` varchar(25) DEFAULT NULL,
  `sexo` varchar(15) DEFAULT NULL,
  `fechaNac` date NOT NULL,
  `telefono` varchar(35) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `curso` varchar(45) DEFAULT NULL,
  `seccion` varchar(45) DEFAULT NULL,
  `id_Usr` int(11) NOT NULL,
  `id_Esp` int(11) NOT NULL,
  `id_Emp` int(11) DEFAULT NULL,
  `id_Sup` int(11) DEFAULT NULL,
  `id_PT` int(11) DEFAULT NULL,
  PRIMARY KEY (`id_Al`),
  KEY `fk_Alumno_Empresa1_idx` (`id_Emp`),
  KEY `fk_Alumno_Supervisor1_idx` (`id_Sup`),
  KEY `fk_Alumno_Padre_Tutor1_idx` (`id_PT`),
  KEY `fk_Alumno_Usuario1_idx` (`id_Usr`),
  KEY `fk_Alumno_Especialidad1_idx` (`id_Esp`),
  CONSTRAINT `fk_Alumno_Empresa1` FOREIGN KEY (`id_Emp`) REFERENCES `empresa` (`id_Emp`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Alumno_Especialidad1` FOREIGN KEY (`id_Esp`) REFERENCES `especialidad` (`id_Esp`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Alumno_Padre_Tutor1` FOREIGN KEY (`id_PT`) REFERENCES `padre_tutor` (`id_PT`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Alumno_Supervisor1` FOREIGN KEY (`id_Sup`) REFERENCES `supervisor` (`id_Sup`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Alumno_Usuario1` FOREIGN KEY (`id_Usr`) REFERENCES `usuario` (`id_Usr`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alumno`
--

LOCK TABLES `alumno` WRITE;
/*!40000 ALTER TABLE `alumno` DISABLE KEYS */;
INSERT INTO `alumno` VALUES (1,'Alumno1','hola','1111113','Masculino','2008-04-17','1111111113','alumno1@ctn.com','3ro','A',3,1,NULL,NULL,1),(3,'Test','Alumno','9999999','Masculino','2008-05-10','0981111111','alumnoelec@ctn.com','3ro','A',6,2,NULL,NULL,NULL),(5,'Rodrigo','Cabañas','5412330','Masculino','2008-03-12','0981340221','rcabanas@ctn.com','3ro','A',10,1,NULL,NULL,NULL),(6,'Camila','Benítez','5412331','Femenino','2008-07-04','0985117430','cbenitez@ctn.com','3ro','A',11,2,NULL,NULL,NULL),(7,'Matías','Ayala','5412332','Masculino','2008-01-29','0972806154','mayala@ctn.com','3ro','A',12,4,NULL,NULL,NULL),(8,'Lucía','Ovelar','5412333','Femenino','2008-11-16','0983529077','lovelar@ctn.com','3ro','A',13,5,NULL,NULL,NULL),(9,'Diego','Villalba','5412334','Masculino','2008-05-23','0976441238','dvillalba@ctn.com','3ro','A',14,6,NULL,3,NULL),(10,'Tamara','Escobar','5412335','Femenino','2008-09-08','0991073365','tescobar@ctn.com','3ro','A',15,7,NULL,NULL,NULL),(11,'Joaquín','Franco','5412336','Masculino','2008-02-14','0984692510','jfranco@ctn.com','3ro','A',16,8,NULL,NULL,NULL),(12,'Belén','Zárate','5412337','Femenino','2008-06-30','0975238841','bzarate@ctn.com','3ro','A',17,9,NULL,NULL,NULL);
/*!40000 ALTER TABLE `alumno` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `documento_scan`
--

DROP TABLE IF EXISTS `documento_scan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `documento_scan` (
  `id_DS` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `ruta` varchar(200) NOT NULL,
  `fecha_subida` datetime(6) NOT NULL,
  `id_Al` int(11) NOT NULL,
  `tipo_documento` varchar(50) DEFAULT NULL,
  `hash_integridad` varchar(64) DEFAULT NULL,
  `validado` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id_DS`),
  KEY `fk_Documento_Scan_Alumno1_idx` (`id_Al`),
  KEY `idx_documento_scan_tipo` (`tipo_documento`),
  KEY `idx_documento_scan_validado` (`validado`),
  CONSTRAINT `fk_Documento_Scan_Alumno1` FOREIGN KEY (`id_Al`) REFERENCES `alumno` (`id_Al`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documento_scan`
--

LOCK TABLES `documento_scan` WRITE;
/*!40000 ALTER TABLE `documento_scan` DISABLE KEYS */;
/*!40000 ALTER TABLE `documento_scan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `empresa`
--

DROP TABLE IF EXISTS `empresa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `empresa` (
  `id_Emp` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) DEFAULT NULL,
  `ruc` varchar(25) DEFAULT NULL,
  `telefono` varchar(35) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `direccion` varchar(100) DEFAULT NULL,
  `id_Esp` int(11) DEFAULT NULL,
  PRIMARY KEY (`id_Emp`),
  KEY `fk_Empresa_Especialidad1` (`id_Esp`),
  CONSTRAINT `fk_Empresa_Especialidad1` FOREIGN KEY (`id_Esp`) REFERENCES `especialidad` (`id_Esp`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `empresa`
--

LOCK TABLES `empresa` WRITE;
/*!40000 ALTER TABLE `empresa` DISABLE KEYS */;
/*!40000 ALTER TABLE `empresa` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `especialidad`
--

DROP TABLE IF EXISTS `especialidad`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `especialidad` (
  `id_Esp` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) DEFAULT NULL,
  `id_Ad` int(11) DEFAULT NULL,
  PRIMARY KEY (`id_Esp`),
  KEY `fk_Especialidad_Administrador1_idx` (`id_Ad`),
  CONSTRAINT `fk_Especialidad_Administrador1` FOREIGN KEY (`id_Ad`) REFERENCES `administrador` (`id_Ad`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `especialidad`
--

LOCK TABLES `especialidad` WRITE;
/*!40000 ALTER TABLE `especialidad` DISABLE KEYS */;
INSERT INTO `especialidad` VALUES (1,'Informática',2),(2,'Electrónica',4),(4,'Construcciones Civiles',9),(5,'Electricidad',10),(6,'Electromecánica',11),(7,'Mecánica Industrial',6),(8,'Mecánica Automotriz',12),(9,'Química Industrial',13);
/*!40000 ALTER TABLE `especialidad` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `menu`
--

DROP TABLE IF EXISTS `menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `menu` (
  `id_Menu` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(45) NOT NULL,
  `predeterminado` tinyint(1) NOT NULL,
  `activo` tinyint(1) NOT NULL,
  PRIMARY KEY (`id_Menu`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menu`
--

LOCK TABLES `menu` WRITE;
/*!40000 ALTER TABLE `menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `padre_tutor`
--

DROP TABLE IF EXISTS `padre_tutor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `padre_tutor` (
  `id_PT` int(11) NOT NULL AUTO_INCREMENT,
  `nombres` varchar(100) DEFAULT NULL,
  `apellidos` varchar(100) DEFAULT NULL,
  `ci` varchar(25) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_PT`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `padre_tutor`
--

LOCK TABLES `padre_tutor` WRITE;
/*!40000 ALTER TABLE `padre_tutor` DISABLE KEYS */;
INSERT INTO `padre_tutor` VALUES (1,'Padre1','Hola','3456789','padreTutor1@ctn.com');
/*!40000 ALTER TABLE `padre_tutor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `planilla_semanal`
--

DROP TABLE IF EXISTS `planilla_semanal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `planilla_semanal` (
  `id_PS` int(11) NOT NULL AUTO_INCREMENT,
  `supervisor` varchar(100) DEFAULT NULL,
  `fecha_desde` date NOT NULL,
  `fecha_hasta` date NOT NULL,
  `total_horas` float(6,2) NOT NULL,
  `conocimientos` varchar(265) DEFAULT NULL,
  `experiencia` varchar(200) DEFAULT NULL,
  `aprendizaje` varchar(200) DEFAULT NULL,
  `id_Al` int(11) NOT NULL,
  PRIMARY KEY (`id_PS`),
  KEY `fk_Planilla_Semanal_Alumno1_idx` (`id_Al`),
  CONSTRAINT `fk_Planilla_Semanal_Alumno1` FOREIGN KEY (`id_Al`) REFERENCES `alumno` (`id_Al`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `planilla_semanal`
--

LOCK TABLES `planilla_semanal` WRITE;
/*!40000 ALTER TABLE `planilla_semanal` DISABLE KEYS */;
INSERT INTO `planilla_semanal` VALUES (2,'Ana Martínez','2026-07-20','2026-07-22',21.50,'Aprendí sobre control de versiones con Git.','Buena adaptación al equipo de trabajo.','Mejoré mis habilidades de resolución de problemas.',1),(7,'Juan perez','2026-07-06','2026-07-06',1.00,'1','2','3',1);
/*!40000 ALTER TABLE `planilla_semanal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `planilla_semanal_detalle`
--

DROP TABLE IF EXISTS `planilla_semanal_detalle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `planilla_semanal_detalle` (
  `id_PSD` int(11) NOT NULL AUTO_INCREMENT,
  `id_PS` int(11) NOT NULL,
  `fecha` date NOT NULL,
  `descripcion` varchar(65) DEFAULT NULL,
  `horas` float(6,2) NOT NULL,
  PRIMARY KEY (`id_PSD`,`id_PS`),
  KEY `fk_Planilla_Semanal_Detalle_Planilla_Semanal1_idx` (`id_PS`),
  CONSTRAINT `fk_Planilla_Semanal_Detalle_Planilla_Semanal1` FOREIGN KEY (`id_PS`) REFERENCES `planilla_semanal` (`id_PS`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `planilla_semanal_detalle`
--

LOCK TABLES `planilla_semanal_detalle` WRITE;
/*!40000 ALTER TABLE `planilla_semanal_detalle` DISABLE KEYS */;
INSERT INTO `planilla_semanal_detalle` VALUES (1,2,'2026-07-20','Reunión de planificación y revisión de tareas.',8.00),(1,7,'2026-07-06','gdgd',1.00),(2,2,'2026-07-21','Desarrollo de módulo de reportes.',6.50),(3,2,'2026-07-22','Pruebas y corrección de errores.',7.00);
/*!40000 ALTER TABLE `planilla_semanal_detalle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supervisor`
--

DROP TABLE IF EXISTS `supervisor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `supervisor` (
  `id_Sup` int(11) NOT NULL AUTO_INCREMENT,
  `nombres` varchar(100) NOT NULL,
  `apellidos` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `id_Esp` int(11) NOT NULL,
  PRIMARY KEY (`id_Sup`),
  KEY `fk_Supervisor_Especialidad1_idx` (`id_Esp`),
  CONSTRAINT `fk_Supervisor_Especialidad1` FOREIGN KEY (`id_Esp`) REFERENCES `especialidad` (`id_Esp`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supervisor`
--

LOCK TABLES `supervisor` WRITE;
/*!40000 ALTER TABLE `supervisor` DISABLE KEYS */;
INSERT INTO `supervisor` VALUES (3,'liliana','salinas','facundovera0702@gmail.com',6);
/*!40000 ALTER TABLE `supervisor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usuario` (
  `id_Usr` int(11) NOT NULL AUTO_INCREMENT,
  `ci` varchar(255) DEFAULT NULL,
  `contrasena` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `activo` tinyint(1) NOT NULL,
  `token_activacion` varchar(100) DEFAULT NULL,
  `token_expiracion` datetime(6) DEFAULT NULL,
  `intentos_codigo` int(11) DEFAULT 0,
  `intentos_login` int(11) DEFAULT 0,
  `bloqueado_hasta` datetime DEFAULT NULL,
  `contrasena_por_defecto` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id_Usr`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES (1,'1111111','$2a$10$Qp51EFs/0zu6RepQlxw8JOEP9C98ix2SPEBUDjrOEts.iVt8r82FS','admin@ctn.com',1,NULL,NULL,0,0,NULL,1),(2,'1111112','$2a$10$mYf1LC/tOAGFUwfiqivJ3u3MHwD6W01Y1vC87VxBIjKJ/vHZoQh46','cordi1@ctn.com',1,NULL,NULL,0,0,NULL,1),(3,'1111113','$2a$10$f5xV7mx02ZqO8KiUImU8gOD1PYg4.C.tZ2I/fS9Z5SGup4I4Bo2su','alumno1@ctn.com',1,NULL,NULL,0,0,NULL,1),(4,'1234567','$2a$10$vHomECwJ4ATfniSQSucMbeHEh4K38YtAjXyqejVlVz1cZxICIvuUK','kevindure4544@gmail.com',1,NULL,NULL,0,0,NULL,1),(5,NULL,'$2b$10$c6RW7r.TnxUnnK3inUpLkuCBYJOxkQiVGxfSMRKki/AOTBSZIahHu','coordiEik@ctn.com',1,NULL,NULL,0,0,NULL,1),(6,'9999999','$2b$10$NWMRPdnMp1pJuZHs9vmTMeuFtozfXMm9KfaH5UCM6Od.x5MtxLH6y','alumnoelec@ctn.com',1,NULL,NULL,0,0,NULL,1),(7,NULL,'$2b$10$NWMRPdnMp1pJuZHs9vmTMeuFtozfXMm9KfaH5UCM6Od.x5MtxLH6y','admin3@ctn.com',1,NULL,NULL,0,0,NULL,1),(8,NULL,'$2b$10$NWMRPdnMp1pJuZHs9vmTMeuFtozfXMm9KfaH5UCM6Od.x5MtxLH6y','coordi2@ctn.com',1,NULL,NULL,0,0,NULL,1),(10,'5412330','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','rcabanas@ctn.com',1,NULL,NULL,0,0,NULL,0),(11,'5412331','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','cbenitez@ctn.com',1,NULL,NULL,0,0,NULL,0),(12,'5412332','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','mayala@ctn.com',1,NULL,NULL,0,0,NULL,0),(13,'5412333','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','lovelar@ctn.com',1,NULL,NULL,0,0,NULL,0),(14,'5412334','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','dvillalba@ctn.com',1,NULL,NULL,0,0,NULL,0),(15,'5412335','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','tescobar@ctn.com',1,NULL,NULL,0,0,NULL,0),(16,'5412336','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','jfranco@ctn.com',1,NULL,NULL,0,0,NULL,0),(17,'5412337','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','bzarate@ctn.com',1,NULL,NULL,0,0,NULL,0),(18,'3874512','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','admin4@ctn.com',1,NULL,NULL,0,0,NULL,0),(20,'3874514','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','coord.construcciones@ctn.com',1,NULL,NULL,0,0,NULL,0),(21,'3874515','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','coord.electricidad@ctn.com',1,NULL,NULL,0,0,NULL,0),(22,'3874516','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','coord.electromecanica@ctn.com',1,NULL,NULL,0,0,NULL,0),(23,'3874517','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','coord.automotriz@ctn.com',1,NULL,NULL,0,0,NULL,0),(24,'3874518','$2a$10$ZcimCdWzem.cq.GITnX6sOFA3kJDoMy3/M2o1GeluXxWWlUR079jy','coord.quimica@ctn.com',1,NULL,NULL,0,0,NULL,0);
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario_menu`
--

DROP TABLE IF EXISTS `usuario_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usuario_menu` (
  `id_Usr` int(11) NOT NULL,
  `id_Menu` int(11) NOT NULL,
  PRIMARY KEY (`id_Usr`,`id_Menu`),
  KEY `fk_Usuario_has_Menu_Menu1_idx` (`id_Menu`),
  KEY `fk_Usuario_has_Menu_Usuario1_idx` (`id_Usr`),
  CONSTRAINT `fk_Usuario_has_Menu_Menu1` FOREIGN KEY (`id_Menu`) REFERENCES `menu` (`id_Menu`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Usuario_has_Menu_Usuario1` FOREIGN KEY (`id_Usr`) REFERENCES `usuario` (`id_Usr`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario_menu`
--

LOCK TABLES `usuario_menu` WRITE;
/*!40000 ALTER TABLE `usuario_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `usuario_menu` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-07 11:00:06
