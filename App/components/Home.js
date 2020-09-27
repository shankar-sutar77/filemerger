import React, { Component } from 'react';
import {
  Platform, StyleSheet, Text, View, Image, ActivityIndicator,
  TextInput, AsyncStorage, ToastAndroid
} from 'react-native';
import { Button } from 'react-native-elements'
import { Icon } from 'react-native-elements'


export default class Home extends Component {

  static navigationOptions = ({ navigation }) => {
    return {
      title: 'Home',
      headerRight: (<Button title={global.userName.toUpperCase()}
        backgroundColor="green"
        buttonStyle={{ height: 10, marginRight: -15 }}
        fontWeight="bold"
        iconRight={{ name: 'user', type: 'evilicon', size: 25 }}
        onPress={this.profile} />)
    };
  };

  logout = () => {
    let webSocket = new WebSocket('ws://' + myip);
    webSocket.onopen = () => {
      let request = {
        action: 'logout',
        username: userName.toLowerCase()
      };
      webSocket.send(JSON.stringify(request));
    };

    webSocket.onmessage = (event) => {
      let response = JSON.parse(event.data);
      if (response.isLogout) {
        this._signOutAsync();
        ToastAndroid.show(
          'User logged out...!',
          ToastAndroid.SHORT
        );
      } else {
        ToastAndroid.show("error occured user logged out...!",
          ToastAndroid.SHORT)
        this._signOutAsync();
      }
    };
    webSocket.onerror = (event) => {
      ToastAndroid.show(event.message, ToastAndroid.LONG);
      console.log("WebSocket error observed:", event);
      this._signOutAsync();
    };
  };

  _signOutAsync = async () => {
    await AsyncStorage.removeItem('userToken');
    userName = '';
    this.props.navigation.navigate('Auth');
  };

  render() {
    const { navigation } = this.props;
    return (
      <View style={styles.container}>
        <Text>Welcome {global.userName}...!</Text>
        <View
          style={{ marginTop: 20 }}>
          <Button title="Send"
            color="white"
            rounded fontSize={18}
            buttonStyle={styles.buttons}
            backgroundColor="#123456"
            iconRight={{ name: 'send', type: 'font-awesome' }}
            onPress={() => {
              navigation.navigate('SplitAndSend', {
                username: global.userName
              })
            }}
          />
        </View>
        <View
          style={{ marginTop: 20 }}>
          <Button title="List of Files"
            color="white"
            buttonStyle={styles.buttons}
            rounded fontSize={18}
            backgroundColor="#123456"
            iconRight={{ name: 'list', type: 'font-awesome' }}
            onPress={() => {
              navigation.navigate("ListOfFiles", {
                username: global.userName
              })
            }}
          />
        </View>
        <View
          style={{ marginTop: 20 }}>
          <Button title="Logout"
            color="white"
            buttonStyle={styles.buttons}
            rounded fontSize={18}
            backgroundColor="#123456"
            iconRight={{ name: 'logout', type: 'material-community' }}
            onPress={this.logout}
          />
        </View>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  buttons: {
    height: 30
  }
});
