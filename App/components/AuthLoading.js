import React, { Component } from 'react';
import {
  NetInfo, ToastAndroid,
  ActivityIndicator,
  AsyncStorage,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import Network from './Network'


export default class AuthLoadingScreen extends Component {
  constructor(props) {
    super(props);
    new Network();
    this._bootstrapAsync();

  }

  _bootstrapAsync = async () => {
    let userToken = await AsyncStorage.getItem('userToken');
    userName = userToken
    console.log("LOGGED IN USER =====> ", userName);
    this.props.navigation.navigate(userToken ? 'App' : 'Auth');
  };

  render() {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" />
        <StatusBar barStyle="default" />
        <Text>Please wait..!</Text>
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
})
