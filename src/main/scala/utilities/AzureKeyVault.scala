package utilities

import com.azure.identity.{AzureCliCredentialBuilder, DefaultAzureCredentialBuilder}
import com.azure.security.keyvault.secrets.SecretClientBuilder
import com.azure.security.keyvault.secrets.models.KeyVaultSecret
import com.azure.core.exception.ResourceNotFoundException

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
    val usingAzureCli = !hostname.contains("cnp-jenkins")

    val credential =
      if (usingAzureCli) {
        println("Using Azure CLI credential")
        new AzureCliCredentialBuilder().build()
      } else {
        println("Using DefaultAzureCredential for Jenkins")
        new DefaultAzureCredentialBuilder().build()
      }

    val client = new SecretClientBuilder()
      .vaultUrl(vaultUrl)
      .credential(credential)
      .buildClient()

    try {
      val secret: KeyVaultSecret = client.getSecret(secretName)
      println(s"Successfully retrieved secret: $secretName from $vaultName")
      secret.getValue
    } catch {
      case e: ResourceNotFoundException =>
        System.err.println(s"Error: Secret '$secretName' not found in Key Vault '$vaultName'.")
        e.printStackTrace()
        sys.exit(1)

      case e: Exception =>
        val message = e.getMessage
        if (usingAzureCli && message.toLowerCase.contains("az login")) {
          System.err.println("[ERROR] Failed to authenticate using Azure CLI.")
          System.err.println("Please run `az login` and try again.")
        } else if (usingAzureCli && message.toLowerCase.contains("Azure CLI not installed")) {
          System.err.println("[ERROR] Azure CLI is not installed.")
          System.err.println("Please install Azure CLI and try again.")
        } else {
          System.err.println(s"[ERROR] Failed to retrieve secret '$secretName' from Key Vault '$vaultName': $message")
        }
        e.printStackTrace()
        sys.exit(1)
    }
  }

}
