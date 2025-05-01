package utils

import com.azure.identity.DefaultAzureCredentialBuilder
import com.azure.security.keyvault.secrets.SecretClientBuilder

object AzureKeyVault {
  def getSecret(secretName: String, vaultName: String): String = {
    val client = new SecretClientBuilder()
      .vaultUrl(s"https://${vaultName}.vault.azure.net/")
      .credential(new DefaultAzureCredentialBuilder().build())
      .buildClient()

    client.getSecret(secretName).getValue
  }
}
