//Created by ODI Studio
/*Use this to run this file in ODI studio*/
//evaluate(new File("C:\\Users\\arjunl\\Documents\\GitHub\\odiscripts\\import_odi.groovy"))

import oracle.odi.core.config.MasterRepositoryDbInfo;
import oracle.odi.core.config.WorkRepositoryDbInfo;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.config.OdiInstanceConfig;
import oracle.odi.core.security.Authentication;
import oracle.odi.core.persistence.transaction.ITransactionDefinition;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.finder.IOdiFolderFinder;
import oracle.odi.impexp.smartie.ISmartExportService;
import oracle.odi.impexp.smartie.ISmartExportable;
import oracle.odi.impexp.smartie.impl.SmartExportServiceImpl;
import oracle.odi.impexp.EncodingOptions;
import java.util.logging.Logger;
import oracle.odi.domain.topology.OdiDataServer;
import oracle.odi.core.config.MasterRepositoryDbInfo;
import oracle.odi.domain.topology.finder.IOdiDataServerFinder;
import oracle.odi.publicapi.samples.SimpleOdiInstanceHandle;
import oracle.odi.impexp.smartie.impl.SmartImportServiceImpl;
import oracle.odi.impexp.smartie.ISmartImportService;
import com.sunopsis.dwg.DwgObject as DwgObject;


Logger logger = Logger.getLogger("")
//logger.info("I am a test info log")
String PROJECT_CODE = 'BIAPPS';
String ODI_DATA_SERVER = 'OBIA_BIA_ODIREPO';
String odiUserConnectionDetailsDataServerName = 'UC_ADMIN_ODI_CONNECTION';
String exportFolderName = 'C:\\Users\\arjunl\\Documents\\odi\\odi_sdk_automationtest\\';
String exportMetaDataFileName = 'C:\\Users\\arjunl\\Documents\\odi\\odi_sdk_automationtest\\ExportMetaData3.dat';
Date processStartTime = new Date();

//Create file for exporting metadata
def exportMetaDataFile = new File(exportMetaDataFileName);
EncodingOptions encodeOptions = new EncodingOptions();

def objectImportResultMap = new HashMap < String, Expando > ();
def objectImportInput = new Expando()

println "Starting Process: " + processStartTime

OdiDataServer odiRepoDataServer = ((IOdiDataServerFinder) odiInstance.getTransactionalEntityManager().getFinder(OdiDataServer.class)).findByName(ODI_DATA_SERVER);
String repoUrl =  odiRepoDataServer.getConnectionSettings().getJdbcUrl();
String repoDriver =  odiRepoDataServer.getConnectionSettings().getDriverName();
String repoUsername = odiRepoDataServer.getUsername();
String repoPassword = DwgObject.snpsDecypher(odiRepoDataServer.getPassword().toString());
String workrepName='BIAPPS_WORKREP';
String odiUsername= ((IOdiDataServerFinder) odiInstance.getTransactionalEntityManager().getFinder(OdiDataServer.class)).findByName(odiUserConnectionDetailsDataServerName).getUsername();
String odiPassword= DwgObject.snpsDecypher(((IOdiDataServerFinder) odiInstance.getTransactionalEntityManager().getFinder(OdiDataServer.class)).findByName(odiUserConnectionDetailsDataServerName).getPassword().toString());
SimpleOdiInstanceHandle MyOdiInstanceHandle = null;
ISmartImportService smartImpSvc =null;


try {

    //Create list objects for export 
	println exportMetaDataFile
    def objectDetails = []
    exportMetaDataFile.eachLine {
        line ->

		objectDetails=line.split(/,/, -1)
		
		//ignore malformed and empty lines 
		if (objectDetails.size()==6)
		{
			objectImportInput = new Expando();
			objectImportInput.srcFolderName = objectDetails[0];
			objectImportInput.exportedSuccessfully = objectDetails[1];
			objectImportInput.exportFileName = exportFolderName + objectDetails[2];
			objectImportInput.internalId = objectDetails[3];
			objectImportInput.result = objectDetails[4];
			objectImportInput.exportTimeString = objectDetails[5];
			objectImportResultMap[objectImportInput.srcFolderName] = objectImportInput;
		}
		else 
		{
			println "Ignoring malformed export metadata line: " + line
		}
    }
	
	println objectImportResultMap
	
	//Transaction Instance

    ITransactionDefinition txnDef = new DefaultTransactionDefinition();
    ITransactionManager tm = odiInstance.getTransactionManager();
    ITransactionStatus txnStatus = tm.getTransaction(txnDef);
	OdiFolder tempOdiFolderObj =null;
	
	
	
		progressIterCount = 0
		objectImportResultMap.each {
		importObject ->
			//Open a new instance and import the object 
			progressIterCount++ 
			MyOdiInstanceHandle = SimpleOdiInstanceHandle
					.create(repoUrl,  //JDBC driver URL
					repoDriver,  //Driver
					repoUsername, //Your Master repository username should come here
					repoPassword, //Your Master repository password should come here
					workrepName, //Work repository name should come here
					odiUsername,  //ODI login username
					odiPassword);   //ODI login password

			tempTxnDef = new DefaultTransactionDefinition();
			tempTm =  MyOdiInstanceHandle.getOdiInstance().getTransactionManager();
			tempTxnStatus = tempTm.getTransaction(tempTxnDef);
	
			smartImpSvc = new SmartImportServiceImpl ( MyOdiInstanceHandle.getOdiInstance())

					try 
					{
						println progressIterCount +"/"+ objectImportResultMap.size()+" Trying to import object " + importObject.value.exportFileName 
						smartImpSvc.importFromXml(importObject.value.exportFileName)
						tempTm.commit(tempTxnStatus)
						println "Imported successfully: " + importObject.value.exportFileName
					}
					catch (e)
					{
						println "Error during Import of: " + importObject.value.exportFileName
						println e
					}
					finally{
					//MyOdiInstanceHandle.release();
					}
				
			MyOdiInstanceHandle.release();
			println "Released instance handle"
			//Close the connection used for import
		}
		
	
	//tm.commit(txnStatus)

	} catch (Exception e) {

    //Commit transaction, Close Authentication and ODI Instance in Exception Block if not used via odi studio

    //auth.close();
    //odiInstance.close();
	println "Result: \n\t" + objectImportResultMap
    println(e);
}
finally {
//if running externally close auth and instance here
//auth.close();
//odiInstance.close();
println "Ending Process: " + new Date()
}