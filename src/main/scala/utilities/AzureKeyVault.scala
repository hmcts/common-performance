package utilities

import com.azure.identity.{AzureCliCredentialBuilder, DefaultAzureCredentialBuilder}
import com.azure.security.keyvault.secrets.SecretClientBuilder

import java.net.InetAddress

object AzureKeyVault {

  /**
   * Resolve the client secret by first checking the environment.
   * If not found, fetch it from Azure Key Vault using the provided vault and secret name.
   */
  def loadClientSecret(vaultName: String, secretName: String): String = {
    sys.env.get("CLIENT_SECRET") match {
      case Some(secret) =>
        println("CLIENT_SECRET loaded from environment")
        secret
      case None =>
        println(s"CLIENT_SECRET not found in environment, fetching from Azure Key Vault: $vaultName / $secretName")
        getSecretFromKeyVault(vaultName, secretName)
    }
  }

  private def getSecretFromKeyVault(vaultName: String, secretName: String): String = {
    val hostname: String = InetAddress.getLocalHost.getHostName
    val vaultUrl = s"https://$vaultName.vault.azure.net"

    val credential =
      if (!hostname.contains("cnp-jenkins")) {
        println("Using Azure CLI credentials")
        new AzureCliCredentialBuilder().build()
      } else {
        println("Using DefaultAzureCredential for Jenkins")
        new DefaultAzureCredentialBuilder().build()
      }

    val client = new SecretClientBuilder()
      .vaultUrl(vaultUrl)
      .credential(credential)
      .buildClient()

    client.getSecret(secretName).getValue
  }

}
