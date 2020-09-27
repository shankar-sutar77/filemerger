import React, { Component } from 'react';
import {
  Platform, StyleSheet, Text, View, Modal,
  TextInput, AsyncStorage, ToastAndroid, Alert
} from 'react-native';
import {
  createStackNavigator,
} from 'react-navigation';
import { Button } from 'react-native-elements'
import { Icon } from 'react-native-elements'

export default class Login extends Component {
  static navigationOptions = {
    title: 'Welcome',
  };
  constructor(props) {
    super(props);
    this.state = {
      username: '',
      ip: '192.168.',
      modalVisible: false
    }
  };

  showModal = () => {
    this.setState({ modalVisible: true });
  };

  setupIP = (IP) => {
    if (IP == '192.168.') {
      alert('Please enter the ip:port..!')
    } else {
      global.myip = IP;
      this.setState({ modalVisible: false });
    }
  };

  validateUser = () => {
    if (global.myip == '') {
      alert("Please setup IP:PORT..!");
    } else {
      if (this.state.username == '') {
        alert("Please enter the username.")
      } else {
        let webSocket = new WebSocket('ws://' + myip);
        webSocket.onopen = () => {
          let request = {
            action: 'login',
            username: this.state.username.toLowerCase()
          };
          webSocket.send(JSON.stringify(request));
        };

        webSocket.onmessage = (event) => {
          let response = JSON.parse(event.data);
          if (response.isValid) {
            this._signInAsync();
            ToastAndroid.show('Login successfull...!',
              ToastAndroid.SHORT
            );
          } else {
            alert("User alredy exists..!")
          }
        };

        webSocket.onerror = (event) => {
          ToastAndroid.show(event.message, ToastAndroid.LONG);
          console.log("WebSocket error observed:", event);
        };
      }
    }
  }

  _signInAsync = async () => {
    console.log("Storing AsyncStorage username");
    await AsyncStorage.setItem('userToken', this.state.username);
    userName = this.state.username;
    this.props.navigation.navigate('App');
  };

  render() {
    const { navigate } = this.props.navigation;
    return (
      <View style={styles.container}>

        <Modal
          animationType="slide"
          transparent={true}
          onRequestClose={() => { this.setState({ modalVisible: false }) }}
          visible={this.state.modalVisible}>
          <View style={styles.modalContainer}>
            <Text>Enter the IP:PORT</Text>
            <TextInput
              style={styles.textInput}
              underlineColorAndroid="transparent"
              onChangeText={(text) => this.setState({ ip: text })}
              value={this.state.ip}
            />

            <View style={{ marginTop: 20 }}>
              <Button title='Submit'
                buttonStyle={{ width: 100 }}
                rounded backgroundColor="#123456"
                onPress={() => { this.setupIP(this.state.ip) }}
              />
            </View>

          </View>
        </Modal>

        <Text style={styles.text}> FileMerger </Text>
        <TextInput
          placeholder="username"
          style={styles.textInput}
          textContentType="username"
          underlineColorAndroid="transparent"
          onChangeText={(text) => this.setState({ username: text.trim() })}
          value={this.state.username}
        />
        <View style={{ marginTop: 20 }}>
          <Button title="Login"
            buttonStyle={{ width: 100 }}
            rounded backgroundColor="#123456"
            onPress={this.validateUser}
          />
        </View>
        <View style={{ marginTop: 20 }}>
          <Button title='SetupIP'
            buttonStyle={{ width: 100 }}
            rounded backgroundColor="#123456"
            onPress={this.showModal}
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
    backgroundColor: '#248ECF',
  },
  modalContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'white'
  },
  textInput: {
    height: 40,
    width: 200,
    borderWidth: 1,
    borderColor: 'black',
    marginTop: 20,
    padding: 10
  },
  text: {
    fontSize: 24,
    fontStyle: 'italic',
  }
});
