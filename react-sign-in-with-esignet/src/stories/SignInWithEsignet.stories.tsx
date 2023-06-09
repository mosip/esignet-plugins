import type { Meta, StoryObj } from "@storybook/react";
import { SignInWithEsignet } from "../lib";

const SignInWithEsignetMeta: Meta<typeof SignInWithEsignet> = {
  title: "SignInWithEsignet",
  component: SignInWithEsignet,
  tags: ["autodocs"],
  argTypes: {
    oidcConfig: {
      control: "object",
    },
    buttonConfig: {
      control: "object",
    },
  },
};

export default SignInWithEsignetMeta;

export const SignInWithEsignetStory: StoryObj<typeof SignInWithEsignet> = {
  render: (args) => {
    return (
      <div style={{ margin: "20px" }}>
        <div style={{ padding: "20px" }}>
          <SignInWithEsignet {...args} />
        </div>
      </div>
    );
  },
};

SignInWithEsignetStory.args = {
  oidcConfig: {
    authorizeUri: "https://esignet.dev.mosip.net/authorize",
    redirect_uri: "https://healthservices.dev.mosip.net/userprofile",
    client_id: "88Vjt34c5Twz1oJ",
    scope: "openid profile",
    nonce: "ere973eieljznge2311",
    state: "eree2311",
    acr_values:
      "mosip:idp:acr:generated-code mosip:idp:acr:biometrics mosip:idp:acr:static-code",
    claims_locales: "en",
    display: "page",
    prompt: "consent",
    max_age: 21,
    ui_locales: "en",
  },
  buttonConfig: {
    type: "standard",
    theme: "filled_black",
    shape: "rounded_edges",
    labelText: "Sign in with e-Signet"
  },
};
