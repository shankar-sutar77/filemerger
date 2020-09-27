import React, { Component } from 'react';
import { Platform, StyleSheet, Text, View, Button, Image, ActivityIndicator,
  TextInput, Picker
} from 'react-native';
import RNFS from 'react-native-fs';
import ImagePicker  from 'react-native-image-picker';
import {
  createStackNavigator,
} from 'react-navigation';

export default class SendMedia extends Component {

  static navigationOptions = ({ navigation }) => {
    const username = navigation.getParam('username', 'Shankar');
    return {
      title: 'SendMedia',
      headerRight: (<Button title={username} onPress={this.profile} />)
    };
  };

  constructor(props) {
    super(props);
    this.state = {
      username : '',
      user : '',
      users : []
    }
  }

  componentWillMount = () => {
    let webSocket = new WebSocket('ws://192.168.0.114:3001');
    webSocket.onopen = () => {
      let request = {
        action : 'getUsers',
      };
      webSocket.send(JSON.stringify(request));
    };

    webSocket.onmessage = (event) => {
      this.setState({
        users : JSON.parse(event.data)
      });
    }
  }

  logout = () => {
    const  { navigation } = this.props;
    let webSocket = new WebSocket('ws://192.168.0.114:3001');
    webSocket.onopen = () => {
      let request = {
        action : 'logout',
        username : navigation.getParam('username', 'Guest')
      };
      webSocket.send(JSON.stringify(request));
    };

    webSocket.onmessage = (event) => {
      let response = JSON.parse(event.data);
      if(response.isLogout) {
        this.props.navigation.navigate('Login');
      } else {
        alert("user not logged out...!")
      }
    }
  }

  render() {

    const { navigate } = this.props.navigation;
    return (
      <View style={styles.container}>

      <Picker
      selectedValue = {this.state.user}
      style={{ height: 50, width: 200 }}
      onValueChange={(itemValue, itemIndex) => this.setState({user: itemValue})}>
      {
        Object.values(this.state.users).map( (values) => {
          return(
            <Picker.Item key={values} value={values} label={values} />
          );
        })
      }
      </Picker>

      <View
      style={{marginTop:20}}>
      <Button title = "Select User"
      onPress={this.validateUser}
      />
      </View>
      <View
      style={{marginTop:20}}>
      <Button title = "logout"
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
  textInput : {
    height:40,
    width:200,
    borderWidth :1,
    borderColor: 'gray',
    marginTop:20
  },
  text :{
    fontSize:24
  }
});
