import React, { Component } from 'react';
import { Platform, StyleSheet, Text, View, Button, Image, ActivityIndicator, TextInput
} from 'react-native';
import RNFS from 'react-native-fs';

export default class MergedItem extends Component {

  static navigationOptions = {
    title: 'MergedItem',
  };

  constructor(props) {
    super(props);
    this.state = {
      imageResponse : {}
    };
  }

  render() {
    const {navigation} = this.props;
    const filePath = 'file://' + navigation.getParam('filePath')
    return(
      <View style={styles.container}>
      <Text>Merged Item from Server</Text>
      <Text>{filePath}</Text>
      <Image style = {styles.imageStyle} source={{uri:filePath}} />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex:1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  imageStyle: {
    width:300,
    height:300,
    marginTop:20,
    marginLeft:10,
    borderColor:'blue',
    borderWidth:5,
    borderRadius:10,
  }
});
